// =============================================================================
// scoreboard/index.ts — barrel exports for the tatami spectator display.
// Importing this module also pulls in the scoreboard stylesheet, so consumers
// get fully-styled components by importing from "./scoreboard".
// =============================================================================

export { DisplayView } from "./DisplayView";
export { OverlayView } from "./OverlayView";
export { ScoreboardFrame } from "./ScoreboardFrame";
export { KumiteBoard } from "./KumiteBoard";
export { KataBoard } from "./KataBoard";
export * from "./BoardChrome";

import "./scoreboard.css"; // consumers get styles by importing the module
