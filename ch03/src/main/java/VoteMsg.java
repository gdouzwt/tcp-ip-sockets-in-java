public class VoteMsg {
    private boolean isInquiry;  // true if inquiry; false if vote
    private boolean isResponse;  // true if response from server
    private int candidateID;  // in [0, 1000]
    private long voteCount;  // nonzero only in response

    public static final int MAX_CANDIDATE_ID = 1000;

    public VoteMsg(boolean isInquiry, boolean isResponse, int candidateID, long voteCount) throws IllegalArgumentException {
        // check invariants
        if (voteCount != 0 && !isResponse) {
            throw new IllegalArgumentException("Request vote count must be zero");
        }
        if (candidateID < 0 || candidateID > MAX_CANDIDATE_ID) {
            throw new IllegalArgumentException("Bad Candidate ID: " + candidateID);
        }
        if (voteCount < 0) {
            throw new IllegalArgumentException("Total must be >= zero");
        }
        this.isInquiry = isInquiry;
        this.isResponse = isResponse;
        this.candidateID = candidateID;
        this.voteCount = voteCount;
    }

    public void setInquiry(boolean isInquiry) {
        this.isInquiry = isInquiry;
    }

    public void setResponse(boolean isResponse) {
        this.isResponse = isResponse;
    }

    public boolean isInquiry() {
        return isInquiry;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public void setCandidateID(int candidateID) throws IllegalArgumentException {
        if (candidateID < 0 || candidateID > MAX_CANDIDATE_ID) {
            throw new IllegalArgumentException("Bad Candidate ID: " + candidateID);
        }
        this.candidateID = candidateID;
    }

    public int getCandidateID() {
        return candidateID;
    }

    public void setVoteCount(long count) {
        if ((count != 0 && !isResponse) || count < 0) {
            throw new IllegalArgumentException("Bad vote count");
        }
        this.voteCount = count;
    }

    public long getVoteCount() {
        return voteCount;
    }

    @Override
    public String toString() {
        String res = (isInquiry ? "inquiry" : "vote") + " for candidate " + candidateID;
        if (isResponse) {
            res = "response to " + res + " who now has " + voteCount + " vote(s)";
        }
        return res;
    }
}
