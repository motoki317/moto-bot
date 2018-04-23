package model

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestOAuth2Authorize_TableName(t *testing.T) {
	t.Parallel()
	assert.Equal(t, "oauth2_authorizes", (&OAuth2Authorize{}).TableName())
}