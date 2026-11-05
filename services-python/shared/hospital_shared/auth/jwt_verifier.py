from __future__ import annotations

from typing import Any

from jose import JWTError, jwt


class JwtVerifier:
    """Verifies HS256 tokens issued by auth-service (Keycloak-compatible shape)."""

    def __init__(self, secret: str, algorithms: tuple[str, ...] = ("HS256",)) -> None:
        self._secret = secret
        self._algorithms = list(algorithms)

    def decode(self, token: str) -> dict[str, Any]:
        try:
            return jwt.decode(token, self._secret, algorithms=self._algorithms)
        except JWTError as exc:  # pragma: no cover - trivial
            raise ValueError("invalid token") from exc


def bearer_token(authorization: str | None) -> str | None:
    if not authorization:
        return None
    scheme, _, token = authorization.partition(" ")
    if scheme.lower() != "bearer" or not token:
        return None
    return token


def roles_from_claims(claims: dict[str, Any]) -> set[str]:
    raw = claims.get("roles") or claims.get("realm_access", {}).get("roles") or []
    if isinstance(raw, str):
        return {r.strip() for r in raw.split(",") if r.strip()}
    return {str(r) for r in raw}
