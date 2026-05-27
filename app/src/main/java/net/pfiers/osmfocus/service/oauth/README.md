# OAuth

This package handles the app's OpenStreetMap OAuth 2 flow via AppAuth.

- `OsmAuthRepository` creates authorization requests and persists the serialized AppAuth `AuthState`
  in `Settings`.
- `OsmAuthWrapper` drives the user-facing login flow and token refreshes.
