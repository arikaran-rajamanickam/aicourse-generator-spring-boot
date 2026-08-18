import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useDeleteProject } from "@/lib/queries/projects";

interface Props {
  open: boolean;
  onOpenChange(open: boolean): void;
  projectId: string;
  projectName: string;
  redirectAfter?: boolean;
}

export function DeleteProjectDialog({
  open,
  onOpenChange,
  projectId,
  projectName,
  redirectAfter,
}: Props) {
  const [confirm, setConfirm] = useState("");
  const [deleteCourses, setDeleteCourses] = useState(false);
  const remove = useDeleteProject();
  const navigate = useNavigate();

  useEffect(() => {
    if (!open) {
      setConfirm("");
      setDeleteCourses(false);
    }
  }, [open]);

  const canDelete = confirm.trim() === projectName.trim();

  async function onConfirm() {
    await remove.mutateAsync({ id: projectId, deleteCourses });
    onOpenChange(false);
    if (redirectAfter) navigate("/projects");
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Delete "{projectName}"?</AlertDialogTitle>
          <AlertDialogDescription>
            This action cannot be undone. To confirm, type the project name below.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-3">
          <div>
            <Label htmlFor="confirm-name">Type the project name</Label>
            <Input
              id="confirm-name"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              placeholder={projectName}
              className="mt-1"
            />
          </div>
          <label className="flex items-start gap-2 text-sm text-muted-foreground cursor-pointer mt-4">
            <Checkbox
              checked={deleteCourses}
              onCheckedChange={(c) => setDeleteCourses(c === true)}
            />
            <span>
              Also delete the linked courses. <span className="text-destructive">This cannot be undone.</span>
            </span>
          </label>
        </div>

        <AlertDialogFooter className="mt-6">
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button variant="destructive" disabled={!canDelete || remove.isPending} onClick={onConfirm}>
            {remove.isPending ? "Deleting…" : "Delete project"}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
