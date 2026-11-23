import { createTheme } from '@mui/material/styles';

// Material look matching Acadware's Angular Material admin: blue top bar, dark side navigation, Inter font.
export const SIDEBAR_BG = '#1f2a37';
export const SIDEBAR_TEXT = '#c9d1db';
export const SIDEBAR_ACTIVE = '#ffffff';

export const theme = createTheme({
  palette: {
    primary: { main: '#1565c0' },
    secondary: { main: '#f57c00' },
    background: { default: '#f4f6f9', paper: '#ffffff' },
  },
  typography: {
    fontFamily: 'Inter, Roboto, "Helvetica Neue", Arial, sans-serif',
    button: { textTransform: 'none', fontWeight: 600 },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiTextField: { defaultProps: { size: 'small', fullWidth: true } },
    MuiFormControl: { defaultProps: { size: 'small', fullWidth: true } },
    MuiTableCell: { styleOverrides: { head: { fontWeight: 600, color: '#4b5563', backgroundColor: '#f9fafb' } } },
    MuiPaper: { styleOverrides: { rounded: { borderRadius: 10 } } },
  },
});
