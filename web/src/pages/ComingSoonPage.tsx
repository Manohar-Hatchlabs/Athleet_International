import { Alert } from '@mui/material';
import { useParams } from 'react-router-dom';
import { PageHeader } from '../components/common';

export function ComingSoonPage() {
  const { module } = useParams();
  const title = (module ?? '').replace(/-/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  return (
    <>
      <PageHeader title={title} />
      <Alert severity="info">This module is part of the Acadware parity plan and will be built in a later phase.</Alert>
    </>
  );
}
