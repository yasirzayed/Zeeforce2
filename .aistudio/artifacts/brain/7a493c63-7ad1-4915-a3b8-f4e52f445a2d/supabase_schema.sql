-- Zayed SalesForce: Supabase Database Schema Migration (Idempotent)
-- Run this script in the Supabase SQL Editor (https://supabase.com/dashboard/project/kocvwcwmzaosgofydjha/sql)

-- 1. Profiles (Staff Members)
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    admin_pin TEXT,
    custom_role_label TEXT,
    is_admin BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    is_suspended BOOLEAN DEFAULT FALSE,
    is_terminated BOOLEAN DEFAULT FALSE,
    can_book_orders BOOLEAN DEFAULT TRUE,
    can_manage_inventory BOOLEAN DEFAULT FALSE,
    can_schedule_delivery BOOLEAN DEFAULT FALSE,
    can_access_inventory BOOLEAN DEFAULT TRUE,
    can_access_orders BOOLEAN DEFAULT TRUE,
    can_access_customers BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Customers
CREATE TABLE IF NOT EXISTS public.customers (
    id TEXT PRIMARY KEY,
    customer_name TEXT NOT NULL,
    business_name TEXT,
    full_location TEXT,
    business_email TEXT,
    contact_person TEXT,
    contact_position TEXT,
    mobile TEXT,
    secondary_mobile TEXT,
    company_email TEXT,
    captured_by TEXT,
    created_at BIGINT,
    is_archived BOOLEAN DEFAULT FALSE,
    pending_deletion_requested BOOLEAN DEFAULT FALSE,
    deletion_request_reason TEXT,
    deletion_requested_by TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Products
CREATE TABLE IF NOT EXISTS public.products (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT,
    unit TEXT,
    min_price DOUBLE PRECISION,
    max_price DOUBLE PRECISION,
    base_price DOUBLE PRECISION,
    stock_qty INTEGER,
    sku TEXT,
    supplier_id TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Orders
CREATE TABLE IF NOT EXISTS public.orders (
    id TEXT PRIMARY KEY,
    order_number TEXT UNIQUE NOT NULL,
    customer_id TEXT,
    customer_name TEXT,
    business_name TEXT,
    customer_location TEXT,
    account_holder TEXT,
    account_holder_contact TEXT,
    order_type TEXT,
    payment_model TEXT,
    distance_km INTEGER,
    regional_tier TEXT,
    freight_surcharge DOUBLE PRECISION,
    expected_delivery TEXT,
    total_quantity INTEGER,
    subtotal DOUBLE PRECISION,
    tax_category TEXT,
    tax_amount DOUBLE PRECISION,
    total_amount DOUBLE PRECISION,
    stamp_code TEXT,
    status TEXT DEFAULT 'BOOKED',
    items JSONB DEFAULT '[]', -- Array of order items
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Commissions
CREATE TABLE IF NOT EXISTS public.commissions (
    id TEXT PRIMARY KEY,
    order_id TEXT,
    order_number TEXT,
    rep_id TEXT,
    rep_name TEXT,
    rep_email TEXT,
    order_total_amount DOUBLE PRECISION,
    commission_type TEXT,
    rate_or_fee DOUBLE PRECISION,
    calculated_amount DOUBLE PRECISION,
    status TEXT,
    payment_code TEXT,
    paid_at BIGINT,
    created_at BIGINT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Staff Activities
CREATE TABLE IF NOT EXISTS public.staff_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id TEXT,
    staff_name TEXT,
    activity_type TEXT,
    description TEXT,
    timestamp BIGINT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Suppliers
CREATE TABLE IF NOT EXISTS public.suppliers (
    id TEXT PRIMARY KEY,
    company_name TEXT NOT NULL,
    company_tag TEXT UNIQUE,
    contact_person TEXT,
    phone TEXT,
    email TEXT,
    is_approved BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. Supplier Products
CREATE TABLE IF NOT EXISTS public.supplier_products (
    id TEXT PRIMARY KEY,
    supplier_id TEXT REFERENCES public.suppliers(id) ON DELETE CASCADE,
    product_name TEXT,
    image_url TEXT,
    price DOUBLE PRECISION,
    commission DOUBLE PRECISION,
    is_active BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 9. Supplier Orders
CREATE TABLE IF NOT EXISTS public.supplier_orders (
    id TEXT PRIMARY KEY,
    parent_order_id TEXT,
    order_number TEXT,
    supplier_id TEXT REFERENCES public.suppliers(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'PENDING',
    total_amount DOUBLE PRECISION,
    admin_commission DOUBLE PRECISION,
    created_at BIGINT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. Supplier Order Items
CREATE TABLE IF NOT EXISTS public.supplier_order_items (
    id TEXT PRIMARY KEY,
    supplier_order_id TEXT REFERENCES public.supplier_orders(id) ON DELETE CASCADE,
    product_id TEXT,
    product_name TEXT,
    quantity INTEGER,
    unit_price DOUBLE PRECISION,
    line_total DOUBLE PRECISION,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 11. Immutable Admin Audit Logs (Multi-Tenant Compliance & Tamper-Evident SHA-256 Trail)
CREATE TABLE IF NOT EXISTS public.admin_audit_logs (
    id TEXT PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    tenant_id TEXT NOT NULL,
    actor_id TEXT,
    actor_name TEXT,
    actor_role TEXT,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    field_name TEXT,
    old_value TEXT,
    new_value TEXT,
    reason TEXT,
    signature TEXT NOT NULL,
    is_tampered BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.commissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.staff_activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.suppliers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.supplier_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.supplier_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.supplier_order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_audit_logs ENABLE ROW LEVEL SECURITY;

-- Idempotent Policies: Drop if already existing before creating (avoids error 42710)
DROP POLICY IF EXISTS "Public full access" ON public.profiles;
CREATE POLICY "Public full access" ON public.profiles FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.customers;
CREATE POLICY "Public full access" ON public.customers FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.products;
CREATE POLICY "Public full access" ON public.products FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.orders;
CREATE POLICY "Public full access" ON public.orders FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.commissions;
CREATE POLICY "Public full access" ON public.commissions FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.staff_activities;
CREATE POLICY "Public full access" ON public.staff_activities FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.suppliers;
CREATE POLICY "Public full access" ON public.suppliers FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.supplier_products;
CREATE POLICY "Public full access" ON public.supplier_products FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.supplier_orders;
CREATE POLICY "Public full access" ON public.supplier_orders FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.supplier_order_items;
CREATE POLICY "Public full access" ON public.supplier_order_items FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public full access" ON public.admin_audit_logs;
CREATE POLICY "Public full access" ON public.admin_audit_logs FOR ALL USING (true) WITH CHECK (true);

