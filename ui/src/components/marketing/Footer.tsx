import { Link } from "react-router-dom";
import { Logo } from "@/components/Logo";

interface FooterContent {
  tagline?: string;
  product?: string[];
  company?: string[];
  legal?: string[];
  social?: string[];
}

const defaultFooter = {
  tagline:
    "The AI-first platform for generating, editing, and deploying world-class learning experiences in minutes, not months.",
  product: ["Features", "Integrations", "Pricing", "Changelog"],
  company: ["About", "Blog", "Careers", "Contact"],
  legal: ["Privacy", "Terms", "Security"],
  social: ["Twitter", "GitHub", "Discord"],
};

export function Footer({ content }: { content?: FooterContent }) {
  const merged = {
    tagline: content?.tagline ?? defaultFooter.tagline,
    product: content?.product ?? defaultFooter.product,
    company: content?.company ?? defaultFooter.company,
    legal: content?.legal ?? defaultFooter.legal,
    social: content?.social ?? defaultFooter.social,
  };

  return (
    <footer className="border-t border-white/5 bg-background relative overflow-hidden">
      <div className="absolute inset-0 bg-grid opacity-[0.2]" />
      <div className="container mx-auto px-6 py-16 relative">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-8">
          <div className="col-span-2">
            <Logo />
            <p className="mt-4 text-muted-foreground text-sm max-w-sm leading-relaxed">
              {merged.tagline}
            </p>
          </div>
          <div>
            <h4 className="font-medium mb-4 text-foreground">Product</h4>
            <ul className="space-y-3 text-sm text-muted-foreground">
              {merged.product.map((item) => (
                <li key={item}><Link to="#" className="hover:text-primary transition-colors">{item}</Link></li>
              ))}
            </ul>
          </div>
          <div>
            <h4 className="font-medium mb-4 text-foreground">Company</h4>
            <ul className="space-y-3 text-sm text-muted-foreground">
              {merged.company.map((item) => (
                <li key={item}><Link to="#" className="hover:text-primary transition-colors">{item}</Link></li>
              ))}
            </ul>
          </div>
          <div>
            <h4 className="font-medium mb-4 text-foreground">Legal</h4>
            <ul className="space-y-3 text-sm text-muted-foreground">
              {merged.legal.map((item) => (
                <li key={item}><Link to="#" className="hover:text-primary transition-colors">{item}</Link></li>
              ))}
            </ul>
          </div>
        </div>
        <div className="mt-16 pt-8 border-t border-white/10 flex flex-col md:flex-row items-center justify-between text-sm text-muted-foreground">
          <p>© {new Date().getFullYear()} AI CourseGen Inc. All rights reserved.</p>
          <div className="flex items-center gap-6 mt-4 md:mt-0">
            {merged.social.map((item) => (
              <Link key={item} to="#" className="hover:text-primary transition-colors">{item}</Link>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
