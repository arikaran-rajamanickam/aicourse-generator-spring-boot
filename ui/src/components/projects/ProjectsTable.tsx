import { formatDistanceToNow } from "date-fns";
import { Copy, MoreHorizontal, Pencil, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { Project } from "@/services/projectApi";
import { DeleteProjectDialog } from "./DeleteProjectDialog";
import { useState } from "react";

export function ProjectsTable({ projects }: { projects: Project[] }) {
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [targetProject, setTargetProject] = useState<Project | null>(null);

  const handleDelete = (p: Project) => {
    setTargetProject(p);
    setDeleteOpen(true);
  };

  return (
    <>
      <div className="rounded-xl border border-border bg-card overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead>Name</TableHead>
              <TableHead>Courses</TableHead>
              <TableHead>Updated</TableHead>
              <TableHead className="hidden md:table-cell">Created</TableHead>
              <TableHead className="w-[80px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {projects.map((project) => (
              <TableRow key={project.id} className="group data-[pending]:opacity-70" data-pending={project.id.startsWith("tmp-") || undefined}>
                <TableCell className="font-medium">
                  <Link to={`/projects/${project.id}`} className="hover:underline flex flex-col">
                    <span>{project.name}</span>
                    {project.description && (
                      <span className="text-xs text-muted-foreground line-clamp-1 font-normal mt-1 max-w-sm">
                        {project.description}
                      </span>
                    )}
                  </Link>
                </TableCell>
                <TableCell>
                  <Badge variant="secondary">{project.courses.length}</Badge>
                </TableCell>
                <TableCell className="text-muted-foreground whitespace-nowrap">
                  {formatDistanceToNow(project.updatedAt ? new Date(project.updatedAt) : new Date(), { addSuffix: true })}
                </TableCell>
                <TableCell className="hidden md:table-cell text-muted-foreground whitespace-nowrap">
                  {project.createdAt ? new Date(project.createdAt).toLocaleDateString() : ""}
                </TableCell>
                <TableCell>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" className="h-8 w-8 p-0" aria-label="Open menu">
                        <MoreHorizontal className="h-4 w-4 text-muted-foreground" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem asChild>
                        <Link to={`/projects/${project.id}`}>Open project</Link>
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Pencil className="mr-2 h-4 w-4" /> Rename
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Copy className="mr-2 h-4 w-4" /> Duplicate
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={() => handleDelete(project)}>
                        <Trash2 className="mr-2 h-4 w-4" /> Delete
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {targetProject && (
        <DeleteProjectDialog
          open={deleteOpen}
          onOpenChange={setDeleteOpen}
          projectId={targetProject.id}
          projectName={targetProject.name}
        />
      )}
    </>
  );
}
