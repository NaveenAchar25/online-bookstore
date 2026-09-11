interface PaginationProps {
  page: number; // 0-indexed, matching the backend
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="pagination">
      <button type="button" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
        Previous
      </button>
      <span className="pagination__status">
        Page {page + 1} of {totalPages}
      </span>
      <button type="button" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>
        Next
      </button>
    </div>
  );
}
