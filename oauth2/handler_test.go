package oauth2

import (
	"encoding/json"
	"github.com/labstack/echo"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
)

var (
	testPrivateKey = []byte(`-----BEGIN RSA PRIVATE KEY-----
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
-----END RSA PRIVATE KEY-----
`)

	testPublicKey = []byte(`-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAuDzi3wlc9w65WlpVTiji
4jUKZcrzhDP0KgjUJpezPVYv15vyklnqxYPr4G+9uWDyPVRqrcIJLTIpv5cWmn8z
vfbZWRjp74D6KbilHLbPy81rQBmrKcTEm2HYe8Sboo1+uXX9wOCaVcwwFqsQI4Xr
8yQmiyw3A+nzlGUBlvs4xJ7XqAQVNeIN68gKltUXTmBqNksQzDTxapz3EkqQn6cG
ue+tivbIB4KkoW1zJzJRz3oxhztKEJTSe91ZRf7+ZwvAmZIRcCceUbrRaH8RLueE
ccWV/qvd6OyzfMM+savrCEeUpn1z0M+0evKOr68JW+zux9zYH4HKgDKRL5MyskyZ
pLlojZdoG5o5X7ZCIry/hzw6emOs//vz5EdV94V1jpGJoLxMVZKNUvjWFT3Vnr5C
JimdbKFGZRvriCvjZUZ5xam+4jJ/7r0TwhKf/sLdjCsGoHNq/3C3FEe+R38jl3Rk
FxszYARcNiNlI8+bRY9wNZvAyDVZ/+X7ZnB5zuGwgueWC5IDHT8qUtdTcpBlZgBw
D/GlqDxyRgi9JKVYkIR5C2Tv795EpOD1aKY1Lk3V8s0WWsMH2x7FI8gJoXz7wAGL
rFAlsr9dmOrBUlyvPMDt3zuf+LCXIC2/unasuafejWMfzebq0YMFzi558oOklMhN
Xms95/7E6F2DvJV2DdhpksUCAwEAAQ==
-----END PUBLIC KEY-----
`)
)

func TestResponseType_valid(t *testing.T) {
	t.Parallel()

	cases := [][5]bool{
		{false, false, false, false, false},
		{true, false, false, false, true},
		{false, true, false, false, true},
		{false, false, true, false, true},
		{false, false, false, true, true},
		{true, true, false, false, true},
		{true, false, true, false, true},
		{true, false, false, true, false},
		{false, true, true, false, true},
		{false, true, false, true, false},
		{false, false, true, true, false},
		{true, true, true, false, true},
		{true, true, false, true, false},
		{true, false, true, true, false},
		{false, true, true, true, false},
		{true, true, true, true, false},
	}
	for _, v := range cases {
		rt := responseType{
			Code:    v[0],
			Token:   v[1],
			IDToken: v[2],
			None:    v[3],
		}
		assert.Equal(t, v[4], rt.valid())
	}
}

func TestHandler_TokenEndpointHandler_Failure1(t *testing.T) {
	t.Parallel()

	assert := assert.New(t)
	e := echo.New()
	h := &Handler{Store: NewStoreMock()}

	f := url.Values{}
	f.Set("grant_type", "ああああ")

	req := httptest.NewRequest(echo.POST, "/", strings.NewReader(f.Encode()))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationForm)

	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if assert.NoError(h.TokenEndpointHandler(c)) {
		assert.Equal(http.StatusBadRequest, rec.Code)
		assert.Equal("no-store", rec.Header().Get("Cache-Control"))
		assert.Equal("no-cache", rec.Header().Get("Pragma"))

		res := errorResponse{}
		if assert.NoError(json.NewDecoder(rec.Body).Decode(&res)) {
			assert.Equal(errUnsupportedGrantType, res.ErrorType)
		}
	}
}

func TestHandler_TokenEndpointHandler_Failure2(t *testing.T) {
	t.Parallel()

	assert := assert.New(t)
	e := echo.New()
	h := &Handler{Store: NewStoreMock()}

	req := httptest.NewRequest(echo.POST, "/", nil)

	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	if assert.NoError(h.TokenEndpointHandler(c)) {
		assert.Equal(http.StatusBadRequest, rec.Code)
		assert.Equal("no-store", rec.Header().Get("Cache-Control"))
		assert.Equal("no-cache", rec.Header().Get("Pragma"))

		res := errorResponse{}
		if assert.NoError(json.NewDecoder(rec.Body).Decode(&res)) {
			assert.Equal(errInvalidRequest, res.ErrorType)
		}
	}
}

func TestHandler_LoadKeys(t *testing.T) {
	t.Parallel()
	assert := assert.New(t)
	h := &Handler{}

	assert.Error(h.LoadKeys(testPublicKey, testPrivateKey))
	assert.Error(h.LoadKeys(testPrivateKey, testPrivateKey))
	assert.NoError(h.LoadKeys(testPrivateKey, testPublicKey))
}

func TestHandler_IsOpenIDConnectAvailable(t *testing.T) {
	t.Parallel()
	assert := assert.New(t)
	require := require.New(t)
	h := &Handler{}
	require.Error(h.LoadKeys(testPublicKey, testPrivateKey))
	assert.False(h.IsOpenIDConnectAvailable())
	require.NoError(h.LoadKeys(testPrivateKey, testPublicKey))
	assert.True(h.IsOpenIDConnectAvailable())
}

func TestHandler_DiscoveryHandler(t *testing.T) {
	t.Parallel()
	assert := assert.New(t)
	require := require.New(t)
	h := &Handler{}
	e := echo.New()

	{
		req := httptest.NewRequest(echo.GET, "/", nil)
		rec := httptest.NewRecorder()
		c := e.NewContext(req, rec)
		assert.EqualError(echo.NewHTTPError(http.StatusNotFound), h.DiscoveryHandler(c).Error())
	}

	require.NoError(h.LoadKeys(testPrivateKey, testPublicKey))

	{
		req := httptest.NewRequest(echo.GET, "/", nil)
		rec := httptest.NewRecorder()
		c := e.NewContext(req, rec)
		assert.NoError(h.DiscoveryHandler(c))
	}
}

func TestHandler_PublicKeysHandler(t *testing.T) {
	t.Parallel()
	assert := assert.New(t)
	require := require.New(t)
	h := &Handler{}
	e := echo.New()

	{
		req := httptest.NewRequest(echo.GET, "/", nil)
		rec := httptest.NewRecorder()
		c := e.NewContext(req, rec)
		assert.EqualError(echo.NewHTTPError(http.StatusNotFound), h.DiscoveryHandler(c).Error())
	}

	require.NoError(h.LoadKeys(testPrivateKey, testPublicKey))

	{
		req := httptest.NewRequest(echo.GET, "/", nil)
		rec := httptest.NewRecorder()
		c := e.NewContext(req, rec)
		assert.NoError(h.DiscoveryHandler(c))
	}

}