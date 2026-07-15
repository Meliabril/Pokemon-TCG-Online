export const API_ENDPOINTS = {
  health: '/api/health',
  auth: {
    login: '/api/auth/login',
    refresh: '/api/auth/refresh',
    logout: '/api/auth/logout',
    me: '/api/auth/me',
    verifyAccount: '/api/auth/verify-account',
    resendVerificationCode: '/api/auth/resend-verification-code',
    passwordForgot: '/api/auth/password/forgot',
    passwordVerifyCode: '/api/auth/password/verify-code',
    passwordResetVerified: '/api/auth/password/reset/verified'
  },
  users: {
    register: '/api/users',
    registerLegacy: '/api/users/create',
    profile: '/api/users/me/profile',
    passwordChangeCode: '/api/users/me/password-change/code',
    passwordChangeVerifyCode: '/api/users/me/password-change/verify-code',
    password: '/api/users/me/password'
  },
  cards: {
    list: '/api/cards',
    search: '/api/cards/search',
    importStatusXy1: '/api/cards/import-status/xy1'
  },
  deck: {
    list: '/api/me/decks',
    createRandom: '/api/me/decks/randomize',
    detail: (deckId: string) => `/api/me/decks/${deckId}`,
    cards: (deckId: string) => `/api/me/decks/${deckId}/cards`,
    validation: (deckId: string) => `/api/me/decks/${deckId}/validation`,
    activate: (deckId: string) => `/api/me/decks/${deckId}/activate`,
    randomize: (deckId: string) => `/api/me/decks/${deckId}/randomize`
  },
  games: {
    detail: (gameId: string) => `/api/games/${gameId}`,
    pause: (gameId: string) => `/api/games/${gameId}/pause`,
    resume: (gameId: string) => `/api/games/${gameId}/resume`,
    actions: (gameId: string) => `/api/games/${gameId}/actions`,
    trainerPreview: (gameId: string) => `/api/games/${gameId}/trainer-preview`,
    history: (gameId: string) => `/api/games/${gameId}/history`,
    events: (gameId: string) => `/api/games/${gameId}/events`,
    snapshot: (gameId: string) => `/api/games/${gameId}/snapshot/latest`,
    latestSnapshot: (gameId: string) => `/api/games/${gameId}/snapshot/latest`,
    chat: (gameId: string) => `/api/games/${gameId}/chat`
  },
  matches: {
    stats: '/api/matches/stats',
    history: '/api/matches/history'
  },
  matchmaking: {
    queue: '/api/matchmaking/queue',
    myStatus: '/api/matchmaking/queue/me',
    customQueue: '/api/matchmaking/queue/custom',
    joinCustomQueue: (code: string) => `/api/matchmaking/queue/custom/join/${code}`
  }
} as const;
