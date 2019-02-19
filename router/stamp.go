package router

import (
	"github.com/labstack/echo"
	"github.com/traPtitech/traQ/model"
	"github.com/traPtitech/traQ/rbac/permission"
	"github.com/traPtitech/traQ/utils/validator"
	"net/http"
)

// GetStamps GET /stamps
func (h *Handlers) GetStamps(c echo.Context) error {
	stamps, err := h.Repo.GetAllStamps()
	if err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}
	return c.JSON(http.StatusOK, stamps)
}

// PostStamp POST /stamps
func (h *Handlers) PostStamp(c echo.Context) error {
	userID := getRequestUserID(c)

	// name確認
	name := c.FormValue("name")
	if !validator.NameRegex.MatchString(name) {
		return echo.NewHTTPError(http.StatusBadRequest, "name must be 1-32 characters of a-zA-Z0-9_-")
	}

	// スタンプ名の重複を確認
	if dup, err := h.Repo.IsStampNameDuplicate(name); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	} else if dup {
		return echo.NewHTTPError(http.StatusConflict, "this name has already been used")
	}

	// file確認
	uploadedFile, err := c.FormFile("file")
	if err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, err)
	}

	// file処理
	fileID, err := h.processMultipartFormStampUpload(c, uploadedFile)
	if err != nil {
		return err
	}

	// スタンプ作成
	if _, err := h.Repo.CreateStamp(name, fileID, userID); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusCreated)
}

// GetStamp GET /stamps/:stampID
func (h *Handlers) GetStamp(c echo.Context) error {
	stamp := getStampFromContext(c)
	return c.JSON(http.StatusOK, stamp)
}

// PatchStamp PATCH /stamps/:stampID
func (h *Handlers) PatchStamp(c echo.Context) error {
	user := getRequestUser(c)
	stampID := getRequestParamAsUUID(c, paramStampID)
	stamp := getStampFromContext(c)
	r := getRBAC(c)

	// ユーザー確認
	if stamp.CreatorID != user.ID && !r.IsGranted(user.ID, user.Role, permission.EditStampCreatedByOthers) {
		return echo.NewHTTPError(http.StatusForbidden, "you are not permitted to edit stamp created by others")
	}

	data := model.Stamp{}
	// 名前変更
	name := c.FormValue("name")
	if len(name) > 0 {
		// 権限確認
		if !r.IsGranted(user.ID, user.Role, permission.EditStampName) {
			return echo.NewHTTPError(http.StatusForbidden, "you are not permitted to change stamp name")
		}
		// 名前を検証
		if !validator.NameRegex.MatchString(name) {
			return echo.NewHTTPError(http.StatusBadRequest, "name must be 1-32 characters of a-zA-Z0-9_-")
		}
		// スタンプ名の重複を確認
		if dup, err := h.Repo.IsStampNameDuplicate(name); err != nil {
			c.Logger().Error(err)
			return echo.NewHTTPError(http.StatusInternalServerError)
		} else if dup {
			return echo.NewHTTPError(http.StatusConflict, "this name has already been used")
		}
		data.Name = name
	}

	// 画像変更
	uploadedFile, err := c.FormFile("file")
	if err == nil {
		fileID, err := h.processMultipartFormStampUpload(c, uploadedFile)
		if err != nil {
			return err
		}
		data.FileID = fileID
	} else if err != http.ErrMissingFile {
		return echo.NewHTTPError(http.StatusBadRequest, err)
	}

	// 更新
	if err := h.Repo.UpdateStamp(stampID, data.Name, data.FileID); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusNoContent)
}

// DeleteStamp DELETE /stamps/:stampID
func (h *Handlers) DeleteStamp(c echo.Context) error {
	stampID := getRequestParamAsUUID(c, paramStampID)

	if err := h.Repo.DeleteStamp(stampID); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusNoContent)
}

// GetMessageStamps GET /messages/:messageID/stamps
func (h *Handlers) GetMessageStamps(c echo.Context) error {
	messageID := getRequestParamAsUUID(c, paramMessageID)

	stamps, err := h.Repo.GetMessageStamps(messageID)
	if err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.JSON(http.StatusOK, stamps)
}

// PostMessageStamp POST /messages/:messageID/stamps/:stampID
func (h *Handlers) PostMessageStamp(c echo.Context) error {
	userID := getRequestUserID(c)
	messageID := getRequestParamAsUUID(c, paramMessageID)
	stampID := getRequestParamAsUUID(c, paramStampID)

	// スタンプをメッセージに押す
	if _, err := h.Repo.AddStampToMessage(messageID, stampID, userID); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusNoContent)
}

// DeleteMessageStamp DELETE /messages/:messageID/stamps/:stampID
func (h *Handlers) DeleteMessageStamp(c echo.Context) error {
	userID := getRequestUserID(c)
	messageID := getRequestParamAsUUID(c, paramMessageID)
	stampID := getRequestParamAsUUID(c, paramStampID)

	// スタンプをメッセージから削除
	if err := h.Repo.RemoveStampFromMessage(messageID, stampID, userID); err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.NoContent(http.StatusNoContent)
}

// GetMyStampHistory GET /users/me/stamp-history
func (h *Handlers) GetMyStampHistory(c echo.Context) error {
	userID := getRequestUserID(c)

	history, err := h.Repo.GetUserStampHistory(userID)
	if err != nil {
		c.Logger().Error(err)
		return echo.NewHTTPError(http.StatusInternalServerError)
	}

	return c.JSON(http.StatusOK, history)
}
