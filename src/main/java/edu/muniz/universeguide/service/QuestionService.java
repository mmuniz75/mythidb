package edu.muniz.universeguide.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;
import javax.persistence.Query;

import edu.muniz.universeguide.model.Answer;
import edu.muniz.universeguide.model.Country;
import edu.muniz.universeguide.model.Question;


@ManagedBean
@SessionScoped
public class QuestionService extends Service{
	
	@Inject
	CountryService countryService;
	
	private String question;
	private String ip;
	private String country;
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		if(this.country==null){
			if(country.indexOf("Unknown Country?") ==-1)
				this.country=country.trim();
			else {
				if(!ip.equals("x.x.x.x"))
					this.country = countryService.getCountryFromIP(ip);
				else
					this.country = "Unknown Country";
			}	
		}
	}

	private boolean justFeedback;
	private boolean justThisMonth;
	private Date startDate;
	private Date endDate;
	
	
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
		this.justThisMonth = false;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
		this.justThisMonth = false;
	}

	public boolean isJustThisMonth() {
		return justThisMonth;
	}

	public void setJustThisMonth(boolean justThisMonth) {
		this.justThisMonth = justThisMonth;
		this.endDate = null;
		this.startDate = null;
	}

	private Integer answerId;
	private String questionFilter;
	private String ipFilter;
	
	
	
	public String getIpFilter() {
		return ipFilter;
	}

	public void setIpFilter(String ipFilter) {
		this.ipFilter = ipFilter;
	}

	public String getQuestionFilter() {
		return questionFilter;
	}

	public void setQuestionFilter(String questionFilter) {
		this.questionFilter = questionFilter;
	}

	public Integer getAnswerId() {
		return answerId;
	}

	public void setAnswerId(Integer answerId) {
		this.answerId = answerId;
	}

	public boolean isJustFeedback() {
		return justFeedback;
	}

	public void setJustFeedback(boolean justFeedback) {
		this.justFeedback = justFeedback;
	}

	public QuestionService(){
		this.justFeedback = false;
		this.justThisMonth = true;
	}
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip.trim();
	}

	private List<Answer> answers; 
	
	@Inject
	private AnswerService answerService;
	
	public void ask(){
		answers = answerService.searchAnswers(question);
	}
		
	public List<Answer>getAnswers(){
		return answers;
	}
		
	
	public void setAnswer(Integer answerID) {
		this.object = new Answer();
		super.setObjectID(answerID);
		
		Question question = new Question();
		question.setText(this.question);
		question.setIp(this.ip);
		question.setCountry(this.country);
		
		question.setAnswer(new Answer((Answer)this.object));
		
		this.object = question;
		try{
			super.save();
		}catch(Exception ex){
			ex.fillInStackTrace();
		}
	}


	@Override
	public void setObjectID(Integer objectID) {
		this.object = new Question();
		super.setObjectID(objectID);
	}

	public String getQuestion() {
		return question;
	}


	public void setQuestion(String question) {
		this.question = question;
	}
	
	public void sendFeedback() throws Exception{
		try{
			super.update();
			Question question = (Question)object;
			question.setFeedback("");
		}catch(Exception ex){
			ex.fillInStackTrace();
		}
		this.object = null;
		
	}
	
	public void unChooseAnswer(){
		this.object = null;
	}
	
	public List<Question> getList(){
		StringBuilder sql = new StringBuilder("select obj from Question obj where 1=1 ");
		if(justFeedback)
			sql.append(" and obj.feedback is not null");
		
		if(answerId != null && answerId > 0)
			sql.append(" and obj.answer.id =" + answerId);
		
		if(questionFilter != null && questionFilter.length() > 0)
			sql.append(" and obj.text like '%" + questionFilter + "%'");
		
		if(ipFilter != null && ipFilter.length() > 0)
			sql.append(" and obj.ip like '%" + ipFilter + "%'");
		
		
		if(justThisMonth)		
			sql.append(" and obj.creationDate >= :monthDate");
		
		if(startDate != null)		
			sql.append(" and obj.creationDate >= :startDate");
		
		if(endDate != null)		
			sql.append(" and obj.creationDate <= :endDate");
						
		sql.append(" order by creationdate desc");
		
		Query query = em.createQuery(sql.toString(),Question.class);
	
		if(justThisMonth){
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.DATE, 1);
			cal.set(Calendar.HOUR, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.AM_PM, Calendar.AM);
			query.setParameter("monthDate", cal.getTime());
		}
		
		if(startDate != null)		
			query.setParameter("startDate", startDate);
		
		if(endDate != null)		
			query.setParameter("endDate", endDate);
		
		List<Question> questions = query.getResultList();  
			
		return questions;
	}
	
	public Number getCountQuestions() {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT count(question.id) ");
		sql.append("FROM Question question ");
		return getCount(sql.toString());
	}
	
	
	public Number getCountUsers() {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT count(DISTINCT question.ip) ");
		sql.append("FROM Question question ");
			
		return getCount(sql.toString());
	}
	
	public Number getCountCountries() {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT count(DISTINCT question.country) ");
		sql.append("FROM Question question ");
			
		return getCount(sql.toString());
	}
	
	public List<Question> getFrequentUsers() {
		reset();
		this.object = new Country();
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT new edu.muniz.universeguide.model.Question(question.ip,question.country,count(question.ip)) ");
		sql.append("where ip not in ('','x.x.x.x')");
		sql.append("group by ip,country");
		sql.append("having count(ip) > 10");
		sql.append("order by 3 desc");
		
		return em.createQuery(sql.toString(),Question.class).getResultList();
	}
	
	public Number getCountFrequentUsers() {
		reset();
		this.object = new Country();
		
		StringBuilder sql = new StringBuilder();
		sql.append("select count(*) from ");
		sql.append("(");
		sql.append("select ip,count(ip) from question ");
		sql.append("where ip not in ('','x.x.x.x') ");
		sql.append("group by ip ");
		sql.append("having count(ip) > 10 ");
		sql.append(") ");
		sql.append("as users");
		
		Number count = (Number)em.createNativeQuery(sql.toString()).getSingleResult();
		return count;
	}
	
	
	
	
}