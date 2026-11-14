INSERT INTO system_configuration (config_key, value, description) VALUES
('SCORE_INITIAL', '50', 'Score inicial del estudiante'),
('SCORE_MAX', '50', 'Score máximo'),
('CHECKOUT_ON_TIME_POINTS', '2', 'Puntos por entrega a tiempo y sin daño'),
('SANCTION_POINTS', '5', 'Puntos descontados por sanción'),
('OVERDUE_DAYS_TO_ZERO', '7', 'Días completos hasta llegar al score 0'),
('ABSENCE_DURATION_DAYS', '7', 'Duración calendario de sanción por inasistencia'),
('ABSENCE_MINUTES', '30', 'Minutos mínimos para confirmar inasistencia'),
('RECOVERY_DAYS', '30', 'Días sin infracciones para recuperación'),
('RECOVERY_POINTS', '5', 'Puntos de recuperación'),
('UNBLOCK_SCORE', '5', 'Score después de desbloqueo manual'),
('LOST_LOAN_PERCENT', '50', 'Porcentaje del costo por préstamo perdido');