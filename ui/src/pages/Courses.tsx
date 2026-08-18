import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { BookOpen, Search, Filter, ArrowRight, Loader2, Plus } from "lucide-react";
import { Link } from "react-router-dom";
import { fetchCourses, deleteCourse, updateCourse } from "../services/courseApi";
import { CourseCard } from "../components/CourseCard";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { toast } from "sonner";
import { Course } from "../types/course";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { fetchSharedWithMeInvites, fetchSharedByMeInvites } from "../services/inviteApi";
import { Badge } from "@/components/ui/badge";
import { Users, Share2, Mail } from "lucide-react";

export default function Courses() {
  const queryClient = useQueryClient();
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  const { data: courses = [], isLoading, isError } = useQuery({
    queryKey: ["courses"],
    queryFn: () => fetchCourses(),
  });

  const { data: sharedWithMe = [], isLoading: isLoadingShared } = useQuery({
    queryKey: ["courses-shared-with-me"],
    queryFn: () => fetchSharedWithMeInvites(),
    refetchInterval: false, // Rely on global useNotifications SSE
  });

  const { data: sharedByMe = [], isLoading: isLoadingByMe } = useQuery({
    queryKey: ["courses-shared-by-me"],
    queryFn: () => fetchSharedByMeInvites(),
    refetchInterval: false,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCourse(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["courses"] });
      toast.success("Course deleted successfully");
    },
    onError: () => toast.error("Failed to delete course"),
  });

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      updateCourse(id, { active }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["courses"] });
      toast.success("Course status updated");
    },
    onSettled: () => setTogglingId(null),
  });

  const handleDelete = (id: string, title: string) => {
    if (confirm(`Are you sure you want to delete "${title}"?`)) {
      deleteMutation.mutate(id);
    }
  };

  const handleToggleActive = (course: Course) => {
    setTogglingId(course.id);
    toggleActiveMutation.mutate({ id: course.id, active: !course.active });
  };

  const filteredCourses = courses.filter((c: Course) => 
    c.title.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const mapEnrollmentToCourse = (enr: any): Course => ({
    id: enr.courseId,
    title: enr.courseName,
    description: enr.courseDescription,
    progress: enr.progressPercentage || 0,
    modules: Array(enr.moduleCount || 0).fill({}),
    active: true,
    createdAt: enr.enrolledAt,
    updatedAt: enr.enrolledAt,
  });

  return (
    <div className="mx-auto max-w-7xl animate-fade-in">
      {/* Header */}
      <div className="gradient-header px-8 pb-8 pt-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl gradient-primary">
              <BookOpen className="h-6 w-6 text-primary-foreground" />
            </div>
            <div>
              <h1 className="font-display text-3xl font-bold text-foreground">My Courses</h1>
              <p className="mt-0.5 text-sm text-muted-foreground">Manage and track your AI-generated learning paths.</p>
            </div>
          </div>

          <Button variant="hero" asChild className="w-full md:w-auto">
            <Link to="/create-course" className="gap-2">
              <Plus className="h-4 w-4" />
              Build New Course
            </Link>
          </Button>
        </div>

        {/* Toolbar */}
        <div className="mt-8 flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input 
              placeholder="Search courses..." 
              className="pl-10 h-11 bg-white text-black border-none shadow-lg focus:ring-primary/20 placeholder:text-slate-400"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <Button variant="outline" className="h-11 gap-2">
            <Filter className="h-4 w-4" />
            Filters
          </Button>
        </div>
      </div>

      <div className="px-8">
        <Tabs defaultValue="my-courses" className="w-full">
          <TabsList className="grid w-full max-w-md grid-cols-3 bg-secondary/30 p-1 mb-8">
            <TabsTrigger value="my-courses" className="text-xs font-bold uppercase tracking-widest">My Courses</TabsTrigger>
            <TabsTrigger value="shared-with" className="text-xs font-bold uppercase tracking-widest">Shared With Me</TabsTrigger>
            <TabsTrigger value="shared-by" className="text-xs font-bold uppercase tracking-widest">Shared By Me</TabsTrigger>
          </TabsList>

          <TabsContent value="my-courses">
            {isLoading ? (
              <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-[320px] rounded-3xl animate-pulse bg-muted border border-border" />
                ))}
              </div>
            ) : filteredCourses.length === 0 ? (
              <EmptyState message={searchQuery ? `No courses matching "${searchQuery}"` : "You haven't generated any courses yet."} />
            ) : (
              <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 pb-10">
                {filteredCourses.map((course: Course, i: number) => (
                  <CourseCard
                    key={course.id}
                    course={course}
                    index={i}
                    onDelete={handleDelete}
                    onToggleActive={handleToggleActive}
                    togglingId={togglingId}
                  />
                ))}
              </div>
            )}
          </TabsContent>

          <TabsContent value="shared-with">
            {isLoadingShared ? (
              <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {[1, 2].map((i) => (
                  <div key={i} className="h-[320px] rounded-3xl animate-pulse bg-muted border border-border" />
                ))}
              </div>
            ) : sharedWithMe.filter((enr: any) => enr.inviteStatus === "ACCEPTED").length === 0 ? (
              <EmptyState message="No courses have been shared and accepted by you yet." />
            ) : (
              <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 pb-10">
                {sharedWithMe.filter((enr: any) => enr.inviteStatus === "ACCEPTED").map((enr: any, i: number) => (
                  <CourseCard
                    key={enr.id}
                    course={mapEnrollmentToCourse(enr)}
                    index={i}
                    onDelete={() => {}}
                    onToggleActive={() => {}}
                    togglingId={null}
                  />
                ))}
              </div>
            )}
          </TabsContent>

          <TabsContent value="shared-by">
            {isLoadingByMe ? (
              <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {[1].map((i) => (
                  <div key={i} className="h-[120px] rounded-2xl animate-pulse bg-muted border border-border" />
                ))}
              </div>
            ) : sharedByMe.length === 0 ? (
              <EmptyState message="You haven't shared any courses with others yet." />
            ) : (
              <div className="space-y-4 pb-10">
                {sharedByMe.map((enr: any) => (
                  <div key={enr.id} className="glass-card flex items-center justify-between p-6 rounded-2xl border border-border/50 hover:border-primary/30 transition-all">
                    <div className="flex items-center gap-4">
                      <div className="h-10 w-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                        <Share2 className="h-5 w-5" />
                      </div>
                      <div>
                        <h4 className="font-display font-bold text-foreground">{enr.courseName}</h4>
                        <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground">
                          <Users className="h-3 w-3" />
                          <span>Shared with <span className="text-foreground font-semibold">{enr.studentName}</span> (@{enr.studentHandle})</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex flex-col items-end gap-2">
                      <Badge variant={enr.inviteStatus === "ACCEPTED" ? "success" : enr.inviteStatus === "PENDING" ? "secondary" : "destructive"} className="text-[10px] font-bold uppercase tracking-widest px-3">
                        {enr.inviteStatus}
                      </Badge>
                      <span className="text-[10px] text-muted-foreground uppercase font-bold tracking-tighter">Sent {new Date(enr.enrolledAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="glass-card rounded-3xl p-16 text-center">
      <div className="h-16 w-16 bg-primary/10 rounded-2xl flex items-center justify-center mx-auto mb-6 text-primary">
        <BookOpen className="h-8 w-8" />
      </div>
      <h3 className="text-xl font-bold text-foreground mb-2">No results found</h3>
      <p className="text-muted-foreground max-w-xs mx-auto">
        {message}
      </p>
    </div>
  );
}
