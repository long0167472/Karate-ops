const fs = require("fs");
const path = require("path");
const express = require("express");

const HOST = process.env.HOST || "0.0.0.0";
const PORT = Number(process.env.PORT || 4173);
const ROOT = __dirname;
const DIST_DIR = path.join(ROOT, "dist");

const app = express();
app.disable("x-powered-by");

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    app: "karate-ops-fe",
    uptime: process.uptime(),
    serverTime: Date.now()
  });
});

if (fs.existsSync(DIST_DIR)) {
  app.use(express.static(DIST_DIR, { etag: true, maxAge: "1h" }));
  app.get("*", (_req, res) => res.sendFile(path.join(DIST_DIR, "index.html")));
} else {
  app.get("*", (_req, res) => {
    res.status(503).send("Frontend build not found. Run `npm install` then `npm run build`, or use `npm run dev`.");
  });
}

app.listen(PORT, HOST, () => {
  console.log(`Karate Ops frontend running at http://localhost:${PORT}`);
});
