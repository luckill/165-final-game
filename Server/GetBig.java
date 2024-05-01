import tage.ai.behaviortrees.BTCondition;

public class GetBig extends BTCondition {
  NPC npc;

  public GetBig(NPC n){
    npc = n;
  }

  protected boolean check(){

    return npc.getBig();
  }

}
