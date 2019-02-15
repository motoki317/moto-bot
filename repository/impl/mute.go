package impl

import (
	"github.com/leandro-lugaresi/hub"
	"github.com/satori/go.uuid"
	"github.com/traPtitech/traQ/event"
	"github.com/traPtitech/traQ/model"
	"github.com/traPtitech/traQ/repository"
)

// MuteChannel 指定したチャンネルをミュートします
func (repo *RepositoryImpl) MuteChannel(userID, channelID uuid.UUID) error {
	if userID == uuid.Nil || channelID == uuid.Nil {
		return repository.ErrNilID
	}
	var m model.Mute
	if err := repo.db.FirstOrCreate(&m, &model.Mute{UserID: userID, ChannelID: channelID}).Error; err != nil {
		return err
	}
	repo.hub.Publish(hub.Message{
		Name: event.ChannelMuted,
		Fields: hub.Fields{
			"user_id":    userID,
			"channel_id": channelID,
		},
	})
	return nil
}

// UnmuteChannel 指定したチャンネルをアンミュートします
func (repo *RepositoryImpl) UnmuteChannel(userID, channelID uuid.UUID) error {
	if userID == uuid.Nil || channelID == uuid.Nil {
		return repository.ErrNilID
	}
	result := repo.db.Delete(&model.Mute{UserID: userID, ChannelID: channelID})
	if result.Error != nil {
		return result.Error
	}
	if result.RowsAffected > 0 {
		repo.hub.Publish(hub.Message{
			Name: event.ChannelUnmuted,
			Fields: hub.Fields{
				"user_id":    userID,
				"channel_id": channelID,
			},
		})
	}
	return nil
}

// GetMutedChannelIDs ミュートしているチャンネルのIDの配列を取得します
func (repo *RepositoryImpl) GetMutedChannelIDs(userID uuid.UUID) (ids []uuid.UUID, err error) {
	ids = make([]uuid.UUID, 0)
	if userID == uuid.Nil {
		return ids, nil
	}
	return ids, repo.db.Model(&model.Mute{}).Where(&model.Mute{UserID: userID}).Pluck("channel_id", &ids).Error
}

// GetMuteUserIDs ミュートしているユーザーのIDの配列を取得します
func (repo *RepositoryImpl) GetMuteUserIDs(channelID uuid.UUID) (ids []uuid.UUID, err error) {
	ids = make([]uuid.UUID, 0)
	if channelID == uuid.Nil {
		return ids, nil
	}
	return ids, repo.db.Model(&model.Mute{}).Where(&model.Mute{ChannelID: channelID}).Pluck("user_id", &ids).Error
}

// IsChannelMuted 指定したユーザーが指定したチャンネルをミュートしているかどうかを返します
func (repo *RepositoryImpl) IsChannelMuted(userID, channelID uuid.UUID) (bool, error) {
	if userID == uuid.Nil || channelID == uuid.Nil {
		return false, nil
	}
	c := 0
	err := repo.db.
		Model(&model.Mute{}).
		Where(&model.Mute{UserID: userID, ChannelID: channelID}).
		Limit(1).
		Count(&c).
		Error
	return c > 0, err
}
