package middlewares

import (
	"context"
	"github.com/gofrs/uuid"
	"github.com/labstack/echo/v4"
	"github.com/traPtitech/traQ/repository"
	"github.com/traPtitech/traQ/router/consts"
	"github.com/traPtitech/traQ/router/extension"
	"github.com/traPtitech/traQ/router/extension/herror"
	"github.com/traPtitech/traQ/router/sessions"
)

const authScheme = "Bearer"

// UserAuthenticate リクエスト認証ミドルウェア
func UserAuthenticate(repo repository.Repository) echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c echo.Context) error {
			var uid uuid.UUID

			if ah := c.Request().Header.Get(echo.HeaderAuthorization); len(ah) > 0 {
				// AuthorizationヘッダーがあるためOAuth2で検証

				// Authorizationスキーム検証
				l := len(authScheme)
				if !(len(ah) > l+1 && ah[:l] == authScheme) {
					return herror.Unauthorized("invalid authorization scheme")
				}

				// OAuth2 Token検証
				token, err := repo.GetTokenByAccess(ah[l+1:])
				if err != nil {
					switch err {
					case repository.ErrNotFound:
						return herror.Unauthorized("invalid token")
					default:
						return herror.InternalServerError(err)
					}
				}

				// tokenの有効期限の検証
				if token.IsExpired() {
					return herror.Unauthorized("invalid token")
				}

				c.Set(consts.KeyOAuth2AccessScopes, token.Scopes)
				uid = token.UserID
			} else {
				// Authorizationヘッダーがないためセッションを確認する
				sess, err := sessions.Get(c.Response(), c.Request(), false)
				if err != nil {
					return herror.InternalServerError(err)
				}
				if sess == nil || sess.GetUserID() == uuid.Nil {
					return herror.Unauthorized("You are not logged in")
				}

				uid = sess.GetUserID()
			}

			// ユーザー取得
			user, err := repo.GetUser(uid, true)
			if err != nil {
				return herror.InternalServerError(err)
			}

			// ユーザーアカウント状態を確認
			if !user.IsActive() {
				return herror.Forbidden("this account is currently suspended")
			}

			c.Set(consts.KeyUser, user)
			c.Set(consts.KeyUserID, user.GetID())
			c.SetRequest(c.Request().WithContext(context.WithValue(c.Request().Context(), extension.CtxUserIDKey, user.GetID()))) // SSEストリーマーで使う
			return next(c)
		}
	}
}
