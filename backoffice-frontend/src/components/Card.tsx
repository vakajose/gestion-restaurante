import type { ReactNode } from 'react';

interface CardProps {
  children: ReactNode;
  padding?: 'none' | 'sm' | 'md';
  className?: string;
}

const PADDING_CLASSES: Record<NonNullable<CardProps['padding']>, string> = {
  none: 'overflow-hidden',
  sm: 'p-4',
  md: 'p-5',
};

export function Card({ children, padding = 'md', className = '' }: CardProps) {
  return <div className={`card ${PADDING_CLASSES[padding]} ${className}`}>{children}</div>;
}
