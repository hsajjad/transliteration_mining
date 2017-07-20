import java.util.*;

public class Mnode {
	private String name;
	private int srcPos;
	private int tarPos;
	private ArrayList<Medge> incoming = new ArrayList<Medge>();
	private ArrayList<Medge> outgoing = new ArrayList<Medge>();
	private Double alpha;
	private Double beta;
	private Double viterbi;
	private String viterbiStr;

	public Mnode(String name, Medge incomming, Medge outgoing, int srcPos,
			int tarPos) {
		this.name = name;
		this.incoming.add(incomming);
		this.outgoing.add(outgoing);
		this.srcPos = srcPos;
		this.tarPos = tarPos;
		this.alpha = 0.0;
		this.beta = 0.0;
		this.viterbi = 0.0;
		this.viterbiStr = "";
	}

	public Mnode(String name, int srcPos, int tarPos) {
		this.name = name;
		this.srcPos = srcPos;
		this.tarPos = tarPos;
		this.alpha = 0.0;
		this.beta = 0.0;
		this.viterbi = 0.0;
		this.viterbiStr = "";
	}

	public void setViterbiStr(String viterbiStr) {
		this.viterbiStr = viterbiStr;
	}

	public String getViterbiStr() {
		return this.viterbiStr;
	}

	public void setViterbi(Double viterbi) {
		this.viterbi = viterbi;
	}

	public Double getViterbi() {
		return this.viterbi;
	}

	public void setAlpha(Double alpha) {
		this.alpha = alpha;
	}

	public Double getAlpha() {
		return this.alpha;
	}

	public void setBeta(Double beta) {
		this.beta = beta;
	}

	public Double getBeta() {
		return this.beta;
	}

	public void setIncoming(Medge incomming) {
		this.incoming.add(incomming);
	}

	public ArrayList<Medge> getIncoming() {
		return this.incoming;
	}

	public void setOutgoing(Medge outgoing) {
		this.outgoing.add(outgoing);
	}

	public ArrayList<Medge> getOutgoing() {
		return this.outgoing;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setSrcPos(int srcPos) {
		this.srcPos = srcPos;
	}

	public int getSrcPos() {
		return this.srcPos;
	}

	public void setTarPos(int tarPos) {
		this.tarPos = tarPos;
	}

	public int getTarPos() {
		return this.tarPos;
	}
}