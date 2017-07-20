public class Medge {

	private Mnode startNode;
	private Mnode endNode;
	private String transition;
	private Double transitionProb;
	private Double gemma;

	public Medge(String transition, Double transitionProb, Mnode startNode,
			Mnode endNode) {
		this.startNode = startNode;
		this.endNode = endNode;
		this.transition = transition;
		this.transitionProb = transitionProb;
		this.gemma = 0.0;
	}

	public void setTransitionProb(Double transitionProb) {
		this.transitionProb = transitionProb;
	}

	public Double getTransitionProb() {
		return this.transitionProb;
	}

	public void setGemma(Double gemma) {
		this.gemma = gemma;
	}

	public Double getGemma() {
		return this.gemma;
	}

	public void setStartNode(Mnode startNode) {
		this.startNode = startNode;
	}

	public Mnode getStartNode() {
		return this.startNode;
	}

	public void setEndNode(Mnode endNode) {
		this.endNode = endNode;
	}

	public Mnode getEndNode() {
		return this.endNode;
	}

	public void setTransition(String transition) {
		this.transition = transition;
	}

	public String getTransition() {
		return this.transition;
	}

}