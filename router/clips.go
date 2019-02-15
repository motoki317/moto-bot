package router

import (
	"github.com/satori/go.uuid"
	"github.com/traPtitech/traQ/repository"
	"net/http"
	"time"

	"github.com/labstack/echo"
	"github.com/traPtitech/traQ/model"
)

// GetClips GET /users/me/clips
func (h *Handlers) GetClips(c echo.Context) error {
	type clipMessageForResponse struct {
		FolderID  uuid.UUID           `json:"folderId"`
		ClipID    uuid.UUID           `json:"clipId"`
		ClippedAt time.Time           `json:"clippedAt"`
		Message   *MessageForResponse `json:"message"`
	}

	userID := getRequestUserID(c)

	// クリップ取得
	clips, err := h.Repo.GetClipMessagesByUser(userID)
	if err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	// 整形
	res := make([]*clipMessageForResponse, len(clips))
	for i, v := range clips {
		res[i] = &clipMessageForResponse{
			FolderID:  v.FolderID,
			ClipID:    v.ID,
			ClippedAt: v.CreatedAt,
			Message:   h.formatMessage(&v.Message),
		}
	}

	return c.JSON(http.StatusOK, res)
}

// PostClip POST /users/me/clips
func (h *Handlers) PostClip(c echo.Context) error {
	userID := getRequestUserID(c)

	// リクエスト検証
	req := struct {
		MessageID string `json:"messageId" validate:"uuid,required"`
		FolderID  string `json:"folderId"`
	}{}
	if err := bindAndValidate(c, &req); err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, err)
	}

	// メッセージの存在と可用性を確認
	if _, err := h.validateMessageID(c, uuid.FromStringOrNil(req.MessageID), userID); err != nil {
		return err
	}

	if len(req.FolderID) > 0 {
		// 保存先フォルダが指定されてる場合はフォルダの確認
		folder, err := h.Repo.GetClipFolder(uuid.FromStringOrNil(req.FolderID))
		if err != nil {
			switch err {
			case repository.ErrNotFound:
				return echo.NewHTTPError(http.StatusBadRequest, "the folder is not found")
			default:
				c.Logger().Error(err)
				return echo.NewHTTPError(http.StatusInternalServerError)
			}
		}
		// フォルダがリクエストユーザーのものかを確認
		if folder.UserID != userID {
			return echo.NewHTTPError(http.StatusBadRequest, "the folder is not found")
		}
	} else {
		// 指定されていない場合はデフォルトフォルダを探す
		folders, err := h.Repo.GetClipFolders(userID)
		if err != nil {
			c.Logger().Error(err)
			return echo.NewHTTPError(http.StatusInternalServerError)
		}
		for _, v := range folders {
			if v.Name == "Default" {
				req.FolderID = v.ID.String()
				break
			}
		}
		if len(req.FolderID) == 0 {
			// 存在しなかったのでデフォルトフォルダを作る
			folder, err := h.Repo.CreateClipFolder(userID, "Default")
			if err != nil {
				c.Logger().Error(err)
				return echo.NewHTTPError(http.StatusInternalServerError)
			}
			req.FolderID = folder.ID.String()
		}
	}

	// クリップ作成
	clip, err := h.Repo.CreateClip(uuid.Must(uuid.FromString(req.MessageID)), uuid.Must(uuid.FromString(req.FolderID)), userID)
	if err != nil {
		if isMySQLDuplicatedRecordErr(err) {
			return echo.NewHTTPError(http.StatusBadRequest, "already clipped")
		}
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.JSON(http.StatusCreated, struct {
		ID uuid.UUID `json:"id"`
	}{clip.ID})
}

// GetClip GET /users/me/clips/:clipID
func (h *Handlers) GetClip(c echo.Context) error {
	clipID := getRequestParamAsUUID(c, paramClipID)

	// クリップ取得
	clip, err := h.getClip(c, clipID, true)
	if err != nil {
		return err
	}

	return c.JSON(http.StatusOK, h.formatMessage(&clip.Message))
}

// DeleteClip DELETE /users/me/clips/:clipID
func (h *Handlers) DeleteClip(c echo.Context) error {
	clipID := getRequestParamAsUUID(c, paramClipID)

	// クリップ取得
	if _, err := h.getClip(c, clipID, true); err != nil {
		return err
	}

	// クリップ削除
	if err := h.Repo.DeleteClip(clipID); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusNoContent)
}

// GetClipsFolder GET /users/me/clips/:clipID/folder
func (h *Handlers) GetClipsFolder(c echo.Context) error {
	clipID := getRequestParamAsUUID(c, paramClipID)

	// クリップ取得
	clip, err := h.getClip(c, clipID, true)
	if err != nil {
		return err
	}

	// クリップのフォルダを取得
	folder, err := h.getClipFolder(c, clip.FolderID, false)
	if err != nil {
		return err
	}

	return c.JSON(http.StatusOK, folder)
}

// PutClipsFolder PUT /users/me/clips/:clipID/folder
func (h *Handlers) PutClipsFolder(c echo.Context) error {
	userID := getRequestUserID(c)
	clipID := getRequestParamAsUUID(c, paramClipID)

	// リクエスト検証
	req := struct {
		FolderID string `json:"folderId" validate:"uuid,required"`
	}{}
	if err := bindAndValidate(c, &req); err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, err)
	}

	// クリップ取得
	if _, err := h.getClip(c, clipID, true); err != nil {
		return err
	}

	// 変更先のクリップのフォルダを取得
	folder, err := h.Repo.GetClipFolder(uuid.FromStringOrNil(req.FolderID))
	if err != nil {
		switch err {
		case repository.ErrNotFound:
			return echo.NewHTTPError(http.StatusBadRequest, "the folder is not found")
		default:
			c.Logger().Error(err)
			return echo.NewHTTPError(http.StatusInternalServerError)
		}
	}
	// フォルダがリクエストユーザーのものかを確認
	if folder.UserID != userID {
		return echo.NewHTTPError(http.StatusBadRequest, "the folder is not found")
	}

	// クリップを更新
	if err := h.Repo.ChangeClipFolder(clipID, folder.ID); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusNoContent)
}

// GetClipFolders GET /users/me/clips/folders
func (h *Handlers) GetClipFolders(c echo.Context) error {
	userID := getRequestUserID(c)

	// フォルダ取得
	folders, err := h.Repo.GetClipFolders(userID)
	if err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.JSON(http.StatusOK, folders)
}

// PostClipFolder POST /users/me/clips/folders
func (h *Handlers) PostClipFolder(c echo.Context) error {
	userID := getRequestUserID(c)

	// リクエスト検証
	req := struct {
		Name string `json:"name" validate:"required,max=30"`
	}{}
	if err := bindAndValidate(c, &req); err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, err)
	}

	// フォルダ作成
	folder, err := h.Repo.CreateClipFolder(userID, req.Name)
	if err != nil {
		if isMySQLDuplicatedRecordErr(err) {
			// フォルダ名が重複
			return echo.NewHTTPError(http.StatusConflict, "the name is duplicated")
		}
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.JSON(http.StatusCreated, folder)
}

// GetClipFolder GET /users/me/clips/folders/:folderID
func (h *Handlers) GetClipFolder(c echo.Context) error {
	type clipMessageForResponse struct {
		ClipID    uuid.UUID           `json:"clipId"`
		ClippedAt time.Time           `json:"clippedAt"`
		Message   *MessageForResponse `json:"message"`
	}

	folderID := getRequestParamAsUUID(c, paramFolderID)

	// フォルダ取得
	folder, err := h.getClipFolder(c, folderID, true)
	if err != nil {
		return err
	}

	// クリップ取得
	clips, err := h.Repo.GetClipMessages(folder.ID)
	if err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	// 整形
	res := make([]*clipMessageForResponse, len(clips))
	for i, v := range clips {
		res[i] = &clipMessageForResponse{
			ClipID:    v.ID,
			ClippedAt: v.CreatedAt,
			Message:   h.formatMessage(&v.Message),
		}
	}

	return c.JSON(http.StatusOK, res)
}

// PatchClipFolder PATCH /users/me/clips/folders/:folderID
func (h *Handlers) PatchClipFolder(c echo.Context) error {
	folderID := getRequestParamAsUUID(c, paramFolderID)

	// リクエスト検証
	req := struct {
		Name string `json:"name" validate:"required,max=30"`
	}{}
	if err := bindAndValidate(c, &req); err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, err)
	}

	// フォルダ取得
	if _, err := h.getClipFolder(c, folderID, true); err != nil {
		return err
	}

	// フォルダ更新
	if err := h.Repo.UpdateClipFolderName(folderID, req.Name); err != nil {
		if isMySQLDuplicatedRecordErr(err) {
			// フォルダ名が重複
			return echo.NewHTTPError(http.StatusConflict, "the name is duplicated")
		}
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusNoContent)
}

// DeleteClipFolder DELETE /users/me/clips/folders/:folderID
func (h *Handlers) DeleteClipFolder(c echo.Context) error {
	folderID := getRequestParamAsUUID(c, paramFolderID)

	// フォルダ取得
	if _, err := h.getClipFolder(c, folderID, true); err != nil {
		return err
	}

	// フォルダ削除
	if err := h.Repo.DeleteClipFolder(folderID); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusNoContent)
}

func (h *Handlers) getClip(c echo.Context, clipID uuid.UUID, restrict bool) (*model.Clip, error) {
	userID := getRequestUserID(c)
	// クリップ取得
	clip, err := h.Repo.GetClipMessage(clipID)
	if err != nil {
		switch err {
		case repository.ErrNotFound:
			return nil, echo.NewHTTPError(http.StatusNotFound)
		default:
			c.Logger().Error(err)
			return nil, echo.NewHTTPError(http.StatusInternalServerError)
		}
	}
	// クリップがリクエストユーザーのものかを確認
	if restrict && clip.UserID != userID {
		return nil, echo.NewHTTPError(http.StatusNotFound)
	}
	return clip, nil
}

func (h *Handlers) getClipFolder(c echo.Context, folderID uuid.UUID, restrict bool) (*model.ClipFolder, error) {
	userID := getRequestUserID(c)
	// フォルダ取得
	folder, err := h.Repo.GetClipFolder(folderID)
	if err != nil {
		switch err {
		case repository.ErrNotFound:
			return nil, echo.NewHTTPError(http.StatusNotFound)
		default:
			c.Logger().Error(err)
			return nil, echo.NewHTTPError(http.StatusInternalServerError)
		}
	}
	// フォルダがリクエストユーザーのものかを確認
	if restrict && folder.UserID != userID {
		return nil, echo.NewHTTPError(http.StatusNotFound)
	}
	return folder, nil
}
