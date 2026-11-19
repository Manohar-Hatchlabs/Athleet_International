import { Box, Link, Paper, Typography } from '@mui/material';
import type { ReactNode } from 'react';

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', bgcolor: 'primary.main', p: 2 }}>
      <Paper sx={{ width: '100%', maxWidth: 400, p: 4 }} elevation={6}>
        <Typography variant="h5" fontWeight={700} color="primary" gutterBottom>
          Athleet
        </Typography>
        <Typography variant="subtitle1" color="text.secondary" sx={{ mb: 3 }}>
          {title}
        </Typography>
        {children}
        <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 3 }}>
          By continuing, you agree to our <Link href="#" underline="hover">Terms of Use</Link>
        </Typography>
      </Paper>
    </Box>
  );
}
