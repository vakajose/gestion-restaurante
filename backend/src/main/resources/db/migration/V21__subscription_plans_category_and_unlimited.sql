-- TASK-623: Categorización de planes SaaS y soporte de vigencia ilimitada (duration_days = 0)
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT c.conname INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE t.relname = 'subscription_plans'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) ILIKE '%duration_days%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE subscription_plans DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE subscription_plans
    ADD CONSTRAINT chk_subscription_plans_duration_non_negative CHECK (duration_days >= 0);

ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS plan_category VARCHAR(30) NOT NULL DEFAULT 'PUBLIC_ONBOARDING';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE t.relname = 'subscription_plans'
          AND c.conname = 'chk_subscription_plans_category'
    ) THEN
        ALTER TABLE subscription_plans
            ADD CONSTRAINT chk_subscription_plans_category
            CHECK (plan_category IN ('PUBLIC_ONBOARDING', 'TRIAL', 'CUSTOM_PRIVATE'));
    END IF;
END $$;

UPDATE subscription_plans
SET plan_category = 'TRIAL'
WHERE UPPER(code) = 'TRIAL_14D';
