/**
 * Utility function to show a confirmation dialog
 * Currently uses window.confirm, but can be enhanced with a custom modal
 */

interface ConfirmDeleteOptions {
  title?: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  confirmColor?: string;
  onConfirm: () => void | Promise<void>;
  onCancel?: () => void;
}

export function confirmDelete(options: ConfirmDeleteOptions): void {
  const {
    title = 'Are you sure?',
    description = 'This action cannot be undone.',
    confirmText = 'Delete',
    cancelText = 'Cancel',
    onConfirm,
    onCancel,
  } = options;

  // Create a simple modal for confirmation
  const dialogElement = document.createElement('div');
  dialogElement.className = 'confirm-dialog-overlay';
  dialogElement.innerHTML = `
    <div class="confirm-dialog-card">
      <h2 class="confirm-dialog-title">${title}</h2>
      <p class="confirm-dialog-message">${description}</p>
      <div class="confirm-dialog-actions">
        <button class="confirm-dialog-cancel">${cancelText}</button>
        <button class="confirm-dialog-confirm">${confirmText}</button>
      </div>
    </div>
  `;

  // Add default styles if not already present
  if (!document.querySelector('style[data-confirm-dialog-styles]')) {
    const style = document.createElement('style');
    style.setAttribute('data-confirm-dialog-styles', 'true');
    style.textContent = `
      .confirm-dialog-overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
        animation: fadeIn 0.2s ease-out;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }

      .confirm-dialog-card {
        background: var(--bg-primary, #1a1a1a);
        border: 1px solid var(--border-color, #333);
        border-radius: 0.5rem;
        padding: 2rem;
        max-width: 400px;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
        animation: slideUp 0.3s ease-out;
      }

      @keyframes slideUp {
        from {
          transform: translateY(20px);
          opacity: 0;
        }
        to {
          transform: translateY(0);
          opacity: 1;
        }
      }

      .confirm-dialog-title {
        margin: 0 0 0.5rem 0;
        font-size: 1.25rem;
        font-weight: 600;
        color: var(--text-primary, #fff);
      }

      .confirm-dialog-message {
        margin: 0 0 2rem 0;
        font-size: 0.95rem;
        color: var(--text-secondary, #aaa);
      }

      .confirm-dialog-actions {
        display: flex;
        gap: 1rem;
        justify-content: flex-end;
      }

      .confirm-dialog-cancel,
      .confirm-dialog-confirm {
        padding: 0.5rem 1rem;
        border: none;
        border-radius: 0.375rem;
        font-size: 0.95rem;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
      }

      .confirm-dialog-cancel {
        background: var(--bg-secondary, #2a2a2a);
        color: var(--text-primary, #fff);
      }

      .confirm-dialog-cancel:hover {
        background: var(--bg-tertiary, #3a3a3a);
      }

      .confirm-dialog-confirm {
        background: #ef4444;
        color: white;
      }

      .confirm-dialog-confirm:hover {
        background: #dc2626;
      }
    `;
    document.head.appendChild(style);
  }

  // Add to DOM
  document.body.appendChild(dialogElement);

  // Handle buttons
  const cancelButton = dialogElement.querySelector('.confirm-dialog-cancel') as HTMLButtonElement;
  const confirmButton = dialogElement.querySelector('.confirm-dialog-confirm') as HTMLButtonElement;

  const cleanup = () => {
    dialogElement.remove();
  };

  const handleConfirm = async () => {
    confirmButton.disabled = true;
    try {
      await onConfirm();
    } finally {
      cleanup();
    }
  };

  const handleCancel = () => {
    onCancel?.();
    cleanup();
  };

  cancelButton.addEventListener('click', handleCancel);
  confirmButton.addEventListener('click', handleConfirm);
  dialogElement.addEventListener('click', (e) => {
    if (e.target === dialogElement) {
      handleCancel();
    }
  });

  // Focus on confirm button
  setTimeout(() => confirmButton.focus(), 50);
}

/**
 * Show a simple alert dialog
 */
export function showAlert(message: string, title = 'Alert'): Promise<void> {
  return new Promise((resolve) => {
    const dialogElement = document.createElement('div');
    dialogElement.className = 'confirm-dialog-overlay';
    dialogElement.innerHTML = `
      <div class="confirm-dialog-card">
        <h2 class="confirm-dialog-title">${title}</h2>
        <p class="confirm-dialog-message">${message}</p>
        <div class="confirm-dialog-actions">
          <button class="confirm-dialog-confirm" style="width: 100%;">OK</button>
        </div>
      </div>
    `;

    document.body.appendChild(dialogElement);

    const button = dialogElement.querySelector('.confirm-dialog-confirm') as HTMLButtonElement;
    const cleanup = () => {
      dialogElement.remove();
      resolve();
    };

    button.addEventListener('click', cleanup);
    dialogElement.addEventListener('click', (e) => {
      if (e.target === dialogElement) cleanup();
    });

    setTimeout(() => button.focus(), 50);
  });
}

