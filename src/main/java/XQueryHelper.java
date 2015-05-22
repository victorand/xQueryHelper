import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.xquery.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by http://rhizomik.net/~roberto/
 */
public class XQueryHelper {
    private static final Logger  log = Logger.getLogger(XQueryHelper.class.getName());

    private XQPreparedExpression expr;
    private XQConnection         conn;

    private JAXBContext          jaxbContext;
    private Unmarshaller         jaxbUnmarshaller;

    static final String apiUrlTeams ="http://www.resultados-futbol.com/scripts/api/api.php?tz=Europe/Madrid&format=xml&req=teams&key=0bbe700a8b3bcf94832e2cd9556b8c5e&league=1";
    static final String apiTeamBase = "http://www.resultados-futbol.com/scripts/api/api.php?tz=Europe/Madrid&format=xml&req=team_players&key=0bbe700a8b3bcf94832e2cd9556b8c5e&team=";
    static final String apiUrlMatchActual = "http://www.resultados-futbol.com/scripts/api/api.php?tz=Europe/Madrid&format=xml&req=matchs&key=0bbe700a8b3bcf94832e2cd9556b8c5e&league=1&order=twin";

    static final String teamsXQ =
            //declare variable $doc := doc(\"http://www.resultados-futbol.com/scripts/api/api.php?tz=Europe/Madrid&format=xml&req=get_teams&key=0bbe700a8b3bcf94832e2cd9556b8c5e&filter=espana\");\n"
                    "declare variable $doc external;\n"
                    + "for $t in $doc/teams/team\n"
                    + "return\n"
                    + "<team>\n"
                    +   "<id>{$t/id/text()}</id>"
                    +   "<nameShow>{$t/nameShow/text()}</nameShow>"
                    +   "</team>";

    static final String playerXQ =
            "declare variable $doc external;\n"
            +"for $t in $doc/team_players/player\n" +
                    "return <player>\n" +
                    "        <id>{$t/id/text()}</id>\n"+
                    "        <nick>{$t/nick/text()}</nick>\n" +
                    "        <role>{$t/role/text()}</role>\n" +
                    "        </player>";

    static final String matchXQ =
            "declare variable $doc external;\n"
            + "for $t in $doc/matchs/match\n"
            +       "return <match>\n"
            +       "       <id>{$t/id/text()}</id>\n" +
                    "        <local>{$t/local/text()}</local>\n" +
                    "        <visitor>{$t/visitor/text()}</visitor>\n" +
                    "        <team1Id>{$t/team1/text()}</team1Id>\n" +
                    "        <team2Id>{$t/team2/text()}</team2Id>\n" +
                    "        <local_goals>{$t/local_goals/text()}</local_goals>\n" +
                    "        <visitor_goals>{$t/visitor_goals/text()}</visitor_goals>\n" +
                    "        <winner>{$t/winner/text()}</winner>"+
                    "        </match>";



    @XmlRootElement
    private static class Match{
        @XmlElement String id;
        @XmlElement String local;
        @XmlElement String visitor;
        @XmlElement String team1Id;
        @XmlElement String team2Id;
        @XmlElement String local_goals;
        @XmlElement String visitor_goals;
        @XmlElement String winner;

        @Override
        public String toString(){
            return "idPartit: " + id +"   " + local + "  " + visitor + " |" + local_goals +" " + visitor_goals;
        }

        //Eliminar els salts de linia retornats per el XML de tots els camps
        public void cleanSalts() {
            this.id = this.id.replaceAll("\n","");
            this.local = this.local.replaceAll("\n","");
            this.visitor = this.visitor.replaceAll("\n","");
            this.team1Id = this.team1Id.replaceAll("\n","");
            this.team2Id = this.team2Id.replaceAll("\n","");
            this.local_goals = this.local_goals.replaceAll("\n","");
            this.visitor_goals = this.visitor_goals.replaceAll("\n", "");
            this.winner = this.winner.replaceAll("\n","");
        }
    }




    @XmlRootElement
    private static class Team{
        @XmlElement String id;
        @XmlElement String nameShow;

        @Override
        public String toString(){
            return "id: "+id+" name: "+nameShow;
        }

        public void cleanSalt() {
            this.id = this.id.replaceAll("\n","");
            this.nameShow = this.nameShow.replaceAll("\n","");
        }
    }

    @XmlRootElement
    private static class Player {
        @XmlElement String id;
        @XmlElement String nick;
        @XmlElement String role;
        @XmlElement String teamId;

        @Override
        public String toString() {
            return "id: "+id+" nick: " + nick + " role: " + role + " teamId:" + teamId;
        }

        public void setTeamId(String teamid) {
            this.teamId = teamid;
        }


        //neteja els salts de linea retornats pel XML
        public void cleanSalts() {
                this.id = this.id.replaceAll("\n","");
                this.nick = this.nick.replaceAll("\n","");
                this.role = this.role.replaceAll("\n","");
                this.teamId = this.teamId.replaceAll("\n","");
        }
    }



    XQueryHelper(String xquery, URL url)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, XQException, IOException, JAXBException {
        URLConnection urlconn = url.openConnection();
        urlconn.setReadTimeout(50000);

        XQDataSource xqds = (XQDataSource) Class.forName("net.sf.saxon.xqj.SaxonXQDataSource").newInstance();
  ;
        this.conn = xqds.getConnection();
        this.expr = conn.prepareExpression(xquery);
        this.expr.bindDocument(new javax.xml.namespace.QName("doc"), urlconn.getInputStream(), null, null);
        //this.jaxbContext = JAXBContext.newInstance(Player.class);
        //this.jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    }

    ArrayList<Team> getTeams() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(Team.class);
        this.jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ArrayList<Team> teams = new ArrayList<Team>();
        try {
            XQResultSequence rs = this.expr.executeQuery();
            while(rs.next()){
                XQItem item = rs.getItem();
                Team team = (Team) jaxbUnmarshaller.unmarshal(item.getNode());

                //Netejem els salts de linea del retorn del XML
               /* String idNet = team.id.replaceAll("\n","");
                String nameNet = team.nameShow.replaceAll("\n","");
                team.id = idNet;
                team.nameShow = nameNet;*/
                team.cleanSalt();
                teams.add(team);

            }

        }catch (Exception e){
            log.log(Level.SEVERE,e.getMessage());
        }
        finally {close();}
        return teams;
    }


    ArrayList<Player> getPlayers(String teamId) throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(Player.class);
        this.jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ArrayList<Player> players = new ArrayList<Player>();
        try {
            XQResultSequence rs = this.expr.executeQuery();
            while(rs.next()){
                XQItem item = rs.getItem();
                Player player = (Player) jaxbUnmarshaller.unmarshal(item.getNode());
                player.teamId = teamId.replaceAll("\n","");
                player.cleanSalts();
                players.add(player);
            }

        }catch (Exception e){
            log.log(Level.SEVERE,e.getMessage());
        }
        finally {close();}
        return players;
    }

    ArrayList<Match> getMatch() throws JAXBException {
        ArrayList<Match> matchs = new ArrayList<Match>();
        this.jaxbContext = JAXBContext.newInstance(Match.class);
        this.jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        try {
            XQResultSequence rs = this.expr.executeQuery();
            while(rs.next()){
                XQItem item = rs.getItem();
                Match match = (Match) jaxbUnmarshaller.unmarshal(item.getNode());
                match.cleanSalts();
                matchs.add(match);
            }

        }catch (Exception e){
            log.log(Level.SEVERE,e.getMessage());
        }
        finally {close();}
        return matchs;

    }



    private void close() {
        try {
            this.expr.close();
            this.conn.close();
        } catch (XQException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
        //hello
    }

    public static void main(String[] args) throws JAXBException {


        String jornada = "&round=";
        String[] teamsId = {"69591", "69592", "69590", "69594", "69597","69593", "69596","69600","69894","69599","69598", "69705","69706","69601","69602","69603", "69604","69605","69606","69595" };
        //String teamsId = {""};
        ArrayList<Player> players = new ArrayList<Player>();
        try {
                //get xml de tots els jugadors de cada equip
                for(int i=0; i<teamsId.length;i++) {
                    XQueryHelper xQueryHelperPlayers = new XQueryHelper(playerXQ, new URL(apiTeamBase+teamsId[i]));
                    players.addAll(xQueryHelperPlayers.getPlayers(teamsId[i]));
                    //players = xQueryHelperPlayers.getPlayers();
                }

            for (Player player : players){
                System.out.println(player);
            }
                //get xml de tots els equips de la lliga BBVA
                XQueryHelper xQueryHelperTeam = new XQueryHelper(teamsXQ, new URL(apiUrlTeams));

                ArrayList<Team> teams = xQueryHelperTeam.getTeams();
                for (Team team : teams) {
                 System.out.println(team);
                 }
            //get XML dels resultats d'una jornada
            XQueryHelper xqh = new XQueryHelper(matchXQ,new URL(apiUrlMatchActual+jornada+37));

            ArrayList<Match> matchs = xqh.getMatch();
            for (Match match: matchs){
                System.out.println(match);
            }


        } catch (Exception e){
            e.printStackTrace();
        }
    }
}