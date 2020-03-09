package v3

import (
	"github.com/dgrijalva/jwt-go"
	vd "github.com/go-ozzo/ozzo-validation"
	"github.com/labstack/echo/v4"
	"github.com/skip2/go-qrcode"
	"github.com/traPtitech/traQ/model"
	"github.com/traPtitech/traQ/rbac"
	"github.com/traPtitech/traQ/rbac/permission"
	"github.com/traPtitech/traQ/rbac/role"
	"github.com/traPtitech/traQ/repository"
	"github.com/traPtitech/traQ/router/consts"
	"github.com/traPtitech/traQ/router/extension/herror"
	"github.com/traPtitech/traQ/router/sessions"
	"github.com/traPtitech/traQ/utils"
	"github.com/traPtitech/traQ/utils/validator"
	"gopkg.in/guregu/null.v3"
	"net/http"
	"time"
)

// GetUsers GET /users
func (h *Handlers) GetUsers(c echo.Context) error {
	q := repository.UsersQuery{}

	if !isTrue(c.QueryParam("include-suspended")) {
		q = q.Active()
	}

	users, err := h.Repo.GetUsers(q)
	if err != nil {
		return herror.InternalServerError(err)
	}
	return c.JSON(http.StatusOK, formatUsers(users))
}

// GetMe GET /users/me
func (h *Handlers) GetMe(c echo.Context) error {
	me := getRequestUser(c)

	tags, err := h.Repo.GetUserTagsByUserID(me.ID)
	if err != nil {
		return herror.InternalServerError(err)
	}

	groups, err := h.Repo.GetUserBelongingGroupIDs(me.ID)
	if err != nil {
		return herror.InternalServerError(err)
	}

	var perms []rbac.Permission
	if me.Role == role.Admin {
		perms = permission.List.Array()
	} else {
		perms = h.RBAC.GetGrantedPermissions(me.Role).Array()
	}

	return c.JSON(http.StatusOK, echo.Map{
		"id":          me.ID,
		"bio":         "", // TODO
		"groups":      groups,
		"tags":        formatUserTags(tags),
		"updatedAt":   me.UpdatedAt,
		"lastOnline":  me.LastOnline.Ptr(),
		"twitterId":   me.TwitterID,
		"name":        me.Name,
		"displayName": me.GetResponseDisplayName(),
		"iconFileId":  me.Icon,
		"bot":         me.Bot,
		"state":       me.Status.Int(),
		"permissions": perms,
	})
}

// PatchMeRequest PATCH /users/me リクエストボディ
type PatchMeRequest struct {
	DisplayName null.String `json:"displayName"`
	TwitterID   null.String `json:"twitterId"`
	Bio         null.String `json:"bio"`
}

func (r PatchMeRequest) Validate() error {
	return vd.ValidateStruct(&r,
		vd.Field(&r.DisplayName, vd.RuneLength(0, 64)),
		vd.Field(&r.TwitterID, validator.TwitterIDRule...),
		vd.Field(&r.Bio, vd.RuneLength(0, 1000)),
	)
}

// EditMe PATCH /users/me
func (h *Handlers) EditMe(c echo.Context) error {
	userID := getRequestUserID(c)

	var req PatchMeRequest
	if err := bindAndValidate(c, &req); err != nil {
		return err
	}

	if err := h.Repo.UpdateUser(userID, repository.UpdateUserArgs{DisplayName: req.DisplayName, TwitterID: req.TwitterID, Bio: req.Bio}); err != nil {
		return herror.InternalServerError(err)
	}

	return c.NoContent(http.StatusNoContent)
}

// PutMyPasswordRequest PUT /users/me/password リクエストボディ
type PutMyPasswordRequest struct {
	Password    string `json:"password"`
	NewPassword string `json:"newPassword"`
}

func (r PutMyPasswordRequest) Validate() error {
	return vd.ValidateStruct(&r,
		vd.Field(&r.Password, vd.Required),
		vd.Field(&r.NewPassword, validator.PasswordRuleRequired...),
	)
}

// ChangeMyPassword PUT /users/me/password
func (h *Handlers) PutMyPassword(c echo.Context) error {
	var req PutMyPasswordRequest
	if err := bindAndValidate(c, &req); err != nil {
		return err
	}

	user := getRequestUser(c)

	// パスワード認証
	if err := model.AuthenticateUser(user, req.Password); err != nil {
		return herror.Unauthorized("password is wrong")
	}

	// パスワード変更
	if err := h.Repo.ChangeUserPassword(user.ID, req.NewPassword); err != nil {
		return herror.InternalServerError(err)
	}
	_ = sessions.DestroyByUserID(user.ID) // 全セッションを破棄(強制ログアウト)
	return c.NoContent(http.StatusNoContent)
}

// GetMyQRCode GET /users/me/qr-code
func (h *Handlers) GetMyQRCode(c echo.Context) error {
	user := getRequestUser(c)

	// トークン生成
	now := time.Now()
	deadline := now.Add(5 * time.Minute)
	token, err := utils.Signer.Sign(jwt.MapClaims{
		"iat":         now.Unix(),
		"exp":         deadline.Unix(),
		"userId":      user.ID,
		"name":        user.Name,
		"displayName": user.DisplayName,
	})
	if err != nil {
		return herror.InternalServerError(err)
	}

	if isTrue(c.QueryParam("token")) {
		// 画像じゃなくて生のトークンを返す
		return c.String(http.StatusOK, token)
	}

	// QRコード画像生成
	png, err := qrcode.Encode(token, qrcode.Low, 512)
	if err != nil {
		return herror.InternalServerError(err)
	}
	return c.Blob(http.StatusOK, consts.MimeImagePNG, png)
}

// GetUserIcon GET /users/:userID/icon
func (h *Handlers) GetUserIcon(c echo.Context) error {
	return serveUserIcon(c, h.Repo, getParamUser(c))
}

// ChangeUserIcon PUT /users/:userID/icon
func (h *Handlers) ChangeUserIcon(c echo.Context) error {
	return changeUserIcon(c, h.Repo, getParamAsUUID(c, consts.ParamUserID))
}

// GetMyIcon GET /users/me/icon
func (h *Handlers) GetMyIcon(c echo.Context) error {
	return serveUserIcon(c, h.Repo, getRequestUser(c))
}

// ChangeMyIcon PUT /users/me/icon
func (h *Handlers) ChangeMyIcon(c echo.Context) error {
	return changeUserIcon(c, h.Repo, getRequestUserID(c))
}

// GetMyStampHistory GET /users/me/stamp-history リクエストクエリ
type GetMyStampHistoryRequest struct {
	Limit int `query:"limit"`
}

func (r *GetMyStampHistoryRequest) Validate() error {
	if r.Limit == 0 {
		r.Limit = 100
	}
	return vd.ValidateStruct(r,
		vd.Field(&r.Limit, vd.Min(1), vd.Max(100)),
	)
}

// GetMyStampHistory GET /users/me/stamp-history
func (h *Handlers) GetMyStampHistory(c echo.Context) error {
	var req GetMyStampHistoryRequest
	if err := bindAndValidate(c, &req); err != nil {
		return err
	}

	userID := getRequestUserID(c)
	history, err := h.Repo.GetUserStampHistory(userID, req.Limit)
	if err != nil {
		return herror.InternalServerError(err)
	}

	return c.JSON(http.StatusOK, history)
}

// PostMyFCMDeviceRequest POST /users/me/fcm-device リクエストボディ
type PostMyFCMDeviceRequest struct {
	Token string `json:"token"`
}

func (r PostMyFCMDeviceRequest) Validate() error {
	return vd.ValidateStruct(&r,
		vd.Field(&r.Token, vd.Required, vd.RuneLength(1, 190)),
	)
}

// PostMyFCMDevice POST /users/me/fcm-device
func (h *Handlers) PostMyFCMDevice(c echo.Context) error {
	var req PostMyFCMDeviceRequest
	if err := bindAndValidate(c, &req); err != nil {
		return err
	}

	userID := getRequestUserID(c)
	if _, err := h.Repo.RegisterDevice(userID, req.Token); err != nil {
		switch {
		case repository.IsArgError(err):
			return herror.BadRequest(err)
		default:
			return herror.InternalServerError(err)
		}
	}

	return c.NoContent(http.StatusNoContent)
}

// PutUserPasswordRequest PUT /users/:userID/password リクエストボディ
type PutUserPasswordRequest struct {
	NewPassword string `json:"newPassword"`
}

func (r PutUserPasswordRequest) Validate() error {
	return vd.ValidateStruct(&r,
		vd.Field(&r.NewPassword, validator.PasswordRuleRequired...),
	)
}

// ChangeUserPassword PUT /users/:userID/password
func (h *Handlers) ChangeUserPassword(c echo.Context) error {
	var req PutUserPasswordRequest
	if err := bindAndValidate(c, &req); err != nil {
		return err
	}

	userID := getParamAsUUID(c, consts.ParamUserID)

	if err := h.Repo.ChangeUserPassword(userID, req.NewPassword); err != nil {
		return herror.InternalServerError(err)
	}

	// ユーザーの全セッションを削除
	_ = sessions.DestroyByUserID(userID)
	return c.NoContent(http.StatusNoContent)
}

// GetUser GET /users/:userID
func (h *Handlers) GetUser(c echo.Context) error {
	user := getParamUser(c)

	tags, err := h.Repo.GetUserTagsByUserID(user.ID)
	if err != nil {
		return herror.InternalServerError(err)
	}

	groups, err := h.Repo.GetUserBelongingGroupIDs(user.ID)
	if err != nil {
		return herror.InternalServerError(err)
	}

	return c.JSON(http.StatusOK, formatUserDetail(user, tags, groups))
}

// PatchUserRequest PATCH /users/:userID リクエストボディ
type PatchUserRequest struct {
	DisplayName null.String `json:"displayName"`
	TwitterID   null.String `json:"twitterId"`
	Role        null.String `json:"role"`
	State       null.Int    `json:"state"`
}

func (r PatchUserRequest) Validate() error {
	return vd.ValidateStruct(&r,
		vd.Field(&r.DisplayName, vd.RuneLength(0, 64)),
		vd.Field(&r.TwitterID, validator.TwitterIDRule...),
		vd.Field(&r.Role, vd.RuneLength(0, 30)),
		vd.Field(&r.State, vd.Min(0), vd.Max(2)),
	)
}

// EditUser PATCH /users/:userID
func (h *Handlers) EditUser(c echo.Context) error {
	userID := getParamAsUUID(c, consts.ParamUserID)

	var req PatchUserRequest
	if err := bindAndValidate(c, &req); err != nil {
		return err
	}

	args := repository.UpdateUserArgs{
		DisplayName: req.DisplayName,
		TwitterID:   req.TwitterID,
		Role:        req.Role,
	}
	if req.State.Valid {
		args.UserState.Valid = true
		args.UserState.State = model.UserAccountStatus(req.State.Int64)
	}

	if err := h.Repo.UpdateUser(userID, args); err != nil {
		return herror.InternalServerError(err)
	}

	return c.NoContent(http.StatusNoContent)
}