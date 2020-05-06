package imaging

import (
	"bytes"
	"context"
	"github.com/disintegration/imaging"
	"golang.org/x/sync/semaphore"
	"image"
	"io"
	"time"
)

type defaultProcessor struct {
	c  Config
	sp *semaphore.Weighted
}

func NewProcessor(c Config) Processor {
	return &defaultProcessor{
		c:  c,
		sp: semaphore.NewWeighted(int64(c.Concurrency)),
	}
}

func (p *defaultProcessor) Thumbnail(src io.ReadSeeker) (image.Image, error) {
	return p.Fit(src, p.c.ThumbnailMaxSize.X, p.c.ThumbnailMaxSize.Y)
}

func (p *defaultProcessor) Fit(src io.ReadSeeker, width, height int) (image.Image, error) {
	p.sp.Acquire(context.Background(), 1)
	defer p.sp.Release(1)

	imgCfg, _, err := image.DecodeConfig(src)
	if err != nil {
		if err == image.ErrFormat {
			return nil, ErrInvalidImageSrc
		}
		return nil, err
	}

	// 画素数チェック
	if imgCfg.Width*imgCfg.Height > p.c.MaxPixels {
		return nil, ErrPixelLimitExceeded
	}

	// 先頭に戻す
	if _, err := src.Seek(0, 0); err != nil {
		return nil, err
	}

	// 変換
	orig, err := imaging.Decode(src, imaging.AutoOrientation(true))
	if err != nil {
		return nil, ErrInvalidImageSrc
	}

	if imgCfg.Width > width || imgCfg.Height > height {
		return imaging.Fit(orig, width, height, imaging.Linear), nil
	}
	return orig, nil
}

func (p *defaultProcessor) FitAnimationGIF(src io.Reader, width, height int) (*bytes.Reader, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second) // 10秒以内に終わらないファイルは無効
	defer cancel()

	b, err := ResizeAnimationGIF(ctx, p.c.ImageMagickPath, src, width, height, false)
	if err != nil {
		switch err {
		case context.DeadlineExceeded:
			return nil, ErrTimeout
		default:
			return nil, err
		}
	}
	return b, nil
}
