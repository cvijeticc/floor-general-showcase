import { CtaSection } from "@/components/site/cta-section";
import { Hero } from "@/components/site/home/hero";
import { HowItWorks } from "@/components/site/home/how-it-works";
import { Offer } from "@/components/site/home/offer";
import { Rules } from "@/components/site/home/rules";
import { Team } from "@/components/site/home/team";

export default function HomePage() {
  return (
    <>
      <Hero />
      <Offer />
      <HowItWorks />
      <Rules />
      <Team />
      <CtaSection />
    </>
  );
}
