"use client";

interface ConfirmDialogProps {
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function ConfirmDialog({
  message,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <>
      <div
        className="fixed inset-0 flex items-center justify-center z-50"
        style={{ backgroundColor: "rgba(0, 0, 0, 0.6)" }}
        onClick={onCancel}
      >
        <div
          className="bg-[#111621] border border-[#1a2535] rounded-lg p-6 w-full max-w-sm"
          onClick={(e) => e.stopPropagation()}
        >
          <p className="text-[#e4e8ee] text-sm mb-4">{message}</p>
          <div className="flex justify-end gap-3 mt-2">
            <button
              type="button"
              onClick={onCancel}
              className="text-[#4a5568] text-sm hover:underline"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={onConfirm}
              className="bg-[#ef4444] text-white text-sm px-4 py-2 rounded hover:bg-[#dc2626]"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
