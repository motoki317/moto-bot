package model

import (
	"github.com/satori/go.uuid"
	"time"
)

// MessageReport メッセージレポート構造体
type MessageReport struct {
	ID        uuid.UUID  `gorm:"type:char(36);primary_key"                   json:"id"`
	MessageID uuid.UUID  `gorm:"type:char(36);unique_index:message_reporter" json:"messageId"`
	Reporter  uuid.UUID  `gorm:"type:char(36);unique_index:message_reporter" json:"reporter"`
	Reason    string     `gorm:"type:text"                                   json:"reason"`
	CreatedAt time.Time  `gorm:"precision:6;index"                           json:"createdAt"`
	DeletedAt *time.Time `gorm:"precision:6"                                 json:"-"`
}

// TableName MessageReport構造体のテーブル名
func (*MessageReport) TableName() string {
	return "message_reports"
}
