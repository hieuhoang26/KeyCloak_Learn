## Client Policies

### Architecture

#### Client Access Type ()

Define the type of OIDC client `confidential` , `public`, `bearer-only`

When a client sends an authorization request, a policy is adopted if this client is confidential

Bearer-only is a deprecated client type

**Client authentication**

- `ON` : confidential -- > check `client_id`, `client_secret`
- `OFF` : public
