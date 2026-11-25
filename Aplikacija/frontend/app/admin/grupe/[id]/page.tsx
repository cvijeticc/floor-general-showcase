import type { Metadata } from "next";
import { GroupDetail } from "./group-detail";

export const metadata: Metadata = {
  title: "Grupa",
};

export default async function GroupPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <GroupDetail groupId={Number(id)} />;
}
