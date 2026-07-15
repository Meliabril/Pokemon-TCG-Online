alter table game_action_logs
    alter column payload type ${jsonDocumentColumnType} using payload::${jsonDocumentColumnType};

alter table game_action_logs
    alter column result type ${jsonDocumentColumnType} using result::${jsonDocumentColumnType};

alter table game_state_snapshots
    alter column state_json type ${jsonDocumentColumnType} using state_json::${jsonDocumentColumnType};

alter table game_events
    alter column payload type ${jsonDocumentColumnType} using payload::${jsonDocumentColumnType};

alter table games
    alter column setup_state type ${jsonDocumentColumnType} using setup_state::${jsonDocumentColumnType};

alter table games
    alter column resolution_state type ${jsonDocumentColumnType} using resolution_state::${jsonDocumentColumnType};
