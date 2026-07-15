export type GameUiErrorKind =
  | 'action-not-supported'
  | 'action-rejected'
  | 'version-conflict'
  | 'invalid-snapshot'
  | 'connection-lost'
  | 'load-failed'
  | 'unknown';

export interface GameUiError {
  kind: GameUiErrorKind;
  title?: string;
  message: string;
  detail?: string | null;
  status: number | null;
  code: string | null;
  recoverable: boolean;
}
