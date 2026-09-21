# Shady Art portfolio

## Editing the portfolio

Open `/shady-dashboard` directly. It is intentionally not linked from the public website. Sign in, then add café or nursery projects, select several photos for each project, and update the contact links. Changes are saved in the browser currently in use.

The starter sign-in is `shady@example.com` / `ChangeMe123!`. Before publishing, configure `VITE_SHADY_EMAIL` and `VITE_SHADY_PASSWORD` in a local `.env` file.

> This is a front-end access screen. A truly private dashboard needs server-side authentication plus a database/file-storage service; browser-side credentials and uploaded images cannot be securely protected after deployment.

This template provides a minimal setup to get React working in Vite with HMR and some Oxlint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the Oxlint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and Oxlint's TypeScript related rules in your project.
