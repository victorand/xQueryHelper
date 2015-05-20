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

    //http://www.resultados-futbol.com/scripts/api/api.php?tz=Europe/Madrid&format=xml&req=teams&key=0bbe700a8b3bcf94832e2cd9556b8c5e&league=1

    static final String apiURL = "http://www.resultados-futbol.com/scripts/api/api.php?tz=Europe/Madrid&format=xml&req=get_teams&key=0bbe700a8b3bcf94832e2cd9556b8c5e&filter=espana";
    static final String apiTeamBase = "http://www.resultados-futbol.com/scripts/api/api.php?tz=Europe/Madrid&format=xml&req=team_players&key=0bbe700a8b3bcf94832e2cd9556b8c5e&team=";


    static final String teamsXQ =
            //declare variable $doc := doc(\"http://www.resultados-futbol.com/scripts/api/api.php?tz=Europe/Madrid&format=xml&req=get_teams&key=0bbe700a8b3bcf94832e2cd9556b8c5e&filter=espana\");\n"
                    "declare variable $doc external;\n"
                    + "for $t in $doc/get_teams/teams\n"
                    + "where fn:contains($t/competition_name/text(), \"Liga BBVA\")"
                    + "return\n"
                    + "<team>\n"
                    +   "<id>{$t/id/text()}</id>"
                    +   "<nameShow>{$t/nameShow/text()}</nameShow>"
                    +   "<competition_name>{$t/competition_name/text()}</competition_name>"
                    +   "</team>";

    static final String playerXQ =
            "declare variable $doc external;\n"
            +"for $t in $doc/team_players/player\n" +
                    "return <player>\n" +
                    "        <nick>{$t/nick/text()}</nick>\n" +
                    "        <role>{$t/role/text()}</role>\n" +
                    "        </player>";


    @XmlRootElement
    private static class Team{
        @XmlElement String id;
        @XmlElement String nameShow;
        @XmlElement String competition_name;


        @Override
        public String toString(){
            return "id: "+id+"name: "+nameShow+"liga: "+competition_name;
        }
    }

    @XmlRootElement
    private static class Player{
        @XmlElement String nick;
        @XmlElement String role;

        @Override
        public String toString(){
            return "nick: "+nick+"\n" + "role: " + role;
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
                teams.add(team);

            }

        }catch (Exception e){
            log.log(Level.SEVERE,e.getMessage());
        }
        finally {close();}
        return teams;
    }


    ArrayList<Player> getPlayers() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(Player.class);
        this.jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ArrayList<Player> players = new ArrayList<Player>();
        try {
            XQResultSequence rs = this.expr.executeQuery();
            while(rs.next()){
                XQItem item = rs.getItem();
                Player player = (Player) jaxbUnmarshaller.unmarshal(item.getNode());
                //player.setTeam(t);
                players.add(player);

            }

        }catch (Exception e){
            log.log(Level.SEVERE,e.getMessage());
        }
        finally {close();}
        return players;
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


        String[] teamsId = {"69591", "69592", "69590", "69594", "69597","69593", "69596","69600","69894","69599","69598", "69705", "69604" };
        //String teamsId = {""};
        ArrayList<Player> players = new ArrayList<Player>();
        try {

                for(int i=0; i<teamsId.length;i++) {
                    XQueryHelper xQueryHelperPlayers = new XQueryHelper(playerXQ, new URL(apiTeamBase+teamsId[i]));
                    players.addAll(xQueryHelperPlayers.getPlayers());
                    //players = xQueryHelperPlayers.getPlayers();
                }

            for (Player player : players){
                System.out.println(player);
            }

                XQueryHelper xQueryHelperTeam = new XQueryHelper(teamsXQ, new URL(apiURL));

                ArrayList<Team> teams = xQueryHelperTeam.getTeams();
                for (Team team : teams) {
                 System.out.println(team);
                 }


        } catch (Exception e){
            e.printStackTrace();
        }
    }
}