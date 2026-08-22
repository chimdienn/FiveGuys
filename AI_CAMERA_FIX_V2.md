# AI Camera Fix v2

The previous Gemini request used a Gemini 2.5-style `thinkingBudget = 0`.
When the model was changed to Gemini 3.6 Flash, that payload can be rejected as
`Request contains an invalid argument`.

This version:
- uses `gemini-3.6-flash`;
- removes the incompatible numeric thinking budget entirely;
- keeps JSON output with `responseMimeType = application/json`;
- keeps CameraX image normalization and the longer mobile-network timeout.

Copy your existing private `.env` into the project root before building.
Never commit `.env`.
