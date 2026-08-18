import { LessonTable } from "@/types/lessonContent";
import RichText from "./RichText";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface Props {
  block: LessonTable;
}

export default function LessonTableBlock({ block }: Props) {
  const { headers, rows } = block.content;

  return (
    <div className="my-6 overflow-hidden rounded-lg border border-border">
      <Table>
        <TableHeader>
          <TableRow className="bg-muted/50 hover:bg-muted/50">
            {headers.map((h, i) => (
              <TableHead
                key={i}
                className="font-display font-semibold text-foreground"
              >
                <RichText text={h} />
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.map((row, i) => (
            <TableRow key={i} className="hover:bg-muted/30">
              {row.map((cell, j) => (
                <TableCell key={j} className="text-muted-foreground">
                  <RichText text={cell} />
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
