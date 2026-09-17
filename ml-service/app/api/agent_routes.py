from fastapi import APIRouter, HTTPException
from app.agent.gemini_agent import gemini_agent, AgentQueryRequest, AgentQueryResponse, RiskExplanationRequest

router = APIRouter(prefix="/api/v1/agent", tags=["Gemini AI Agent"])

@router.post("/query", response_model=AgentQueryResponse)
def query_agent(request: AgentQueryRequest):
    """Natural language clinical supply-chain query with native tool calling and grounded reasoning."""
    try:
        return gemini_agent.query(request.query, request.phc_id, request.medicine_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Agent reasoning failed: {str(e)}")

@router.post("/explain-risk")
def explain_risk(request: RiskExplanationRequest):
    """Direct grounded explanation of risk factors for a specific PHC and medicine."""
    try:
        return gemini_agent.explain_risk(request.phc_id, request.medicine_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Risk explanation failed: {str(e)}")
