import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @author Carlos Eduardo Gómez Montoya
 * @author Cristian David Sierra Barrera
 * @author Harold Enrique Castro Barrera
 * Implement a client, which execute commands based on the communication with the server.
 */
public class TCPClient
{
	//---------------------------------------------------
	//	Constants
	//---------------------------------------------------
	public static final String COMMAND_SH = "sh";
	public static final String SPACE = " ";
	public static final String PATH_HOME = "/home/user";
	public static final String CPU = "/cpu.sh";
	public static final String MEMORY = "/memory.sh";
	public static final String DISK = "/disk.sh";
	public static final String IO = "/io.sh";
	public static final String ALL = "/all.sh";
	public static final String BACKGROUND = "&";

	//---------------------------------------------------
	//	Variables
	//---------------------------------------------------
	/**
	 * Buffered Reader with Server
	 */
	private BufferedReader in;
	
	/**
	 * Print Writer with Server
	 */
	private PrintWriter out;
	
	/**
	 * Port to communicate
	 */
	public int port;
	
	/**
	 * Location of Server
	 */
	public String serverLocation;
	
	/**
	 * Socket for Client
	 */
	private Socket             clientSocket;
	
	
	private ObjectOutputStream outToServer;
	private ObjectInputStream  inFromServer;
	private FileOutputStream   outFile;
	private File               file;

	/**
	 * Constructor with Server
	 * @param address of Server
	 */
	public TCPClient ( String address, int port )
	{
		this.serverLocation = address;
		this.port = port;
		try
		{
			// Initializate Socket
			clientSocket = new Socket ( serverLocation, port );
			System.out.println ( "Client running ..." );
			
			// Make connection and initialize Streams
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			System.out.println("Created flows ...");
			System.out.println("Waiting for Server ...");
			
			// Read configuration to execute
			String configuration = in.readLine();
			String script = "";
			if(configuration.equals("CPU")) {
				script = CPU;
			} else if(configuration.equals("MEMORY")) {
				script = MEMORY;
			} else if(configuration.equals("DISK")) {
				script = DISK;
			} else if(configuration.equals("IO")) {
				script = IO;
			} else {
				script = ALL;
			}
			System.out.println("According to server: "+script);
			// Execute the command
			execute(COMMAND_SH+SPACE+PATH_HOME+SPACE+script+SPACE+BACKGROUND);			
			System.out.println("Executing Stress ... "+script+" ... ");
		}
		// Catch Exception of I/O flows
		catch ( IOException e )
		{
			e.printStackTrace ( );
		}
		// Close flows and socket
		finally
		{
			try
			{
				if ( in != null ) in.close();
				if ( out != null ) out.close();
				if ( inFromServer != null ) inFromServer.close ( );
				if ( outToServer  != null ) outToServer.close ( );
				if ( clientSocket != null ) clientSocket.close ( );
			}
			catch ( IOException e )
			{
				e.printStackTrace ( );
			}
		}
		System.out.println ( "OK" );
	}

	/**
	 * Constructor without Server. Execute all available tests from tool stress-ng
	 */
	public TCPClient() {
		execute(COMMAND_SH+SPACE+PATH_HOME+SPACE+ALL+SPACE+BACKGROUND);
	}
	
	//---------------------------------------------------
	//	Methods
	//---------------------------------------------------
	/**
	 * Send an specific object using the flow.
	 */
	private void send ( Object o ) throws IOException
	{
		outToServer.writeObject ( o );
		outToServer.flush ( );
	}

	/**
	 * Receive an specific object using the flow.
	 * @return Object sended
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Object receive ( ) throws IOException, ClassNotFoundException
	{
		return inFromServer.readObject ( );
	}


	/**
	 * Receive a file from server
	 * @param fileName Name of the file.
	 * @throws IOException
	 */
	private void receiveFile ( String fileName ) throws IOException
	{
		try
		{
			// Se crea el archivo con el nombre especificado en la carpeta Download.
			// Observe que esta carpeta debe existir en el host el cliente.
			file = new File ( "Download" + File.separator + fileName );
			outFile = new FileOutputStream ( file );

			// El cliente recibe el nœmero de bloques que compone el archivo.
			int numberOfBlocks = ( ( Integer ) receive ( ) ).intValue ( );

			// Se reciben uno a uno los bloques que conforman el archivo y se
			// almacenan en el archivo.
			for ( int i = 0; i < numberOfBlocks; i++ )
			{
				byte [ ] buffer = ( byte [ ] ) receive ( );
				outFile.write ( buffer, 0, buffer.length );
			}
		}
		// Puede lanzar una excepci—n por clase no encontrada.
		catch ( ClassNotFoundException e )
		{
			e.printStackTrace ( );
		}
		// Puede lanzar una excepci—n de entrada y salida.
		catch ( IOException e )
		{
			e.printStackTrace ( );
		}
		// Finalmente se cierra el archivo.
		finally
		{
			if ( outFile != null ) outFile.close ( );
		}
	}
	
	/**
	 * This method let to create the flows in and out to communicate client and server.
	 * @throws IOException
	 */
	private void crearFlujos ( ) throws IOException
	{
		// Creación del flujo de salida hacia el servidor.
		outToServer = new ObjectOutputStream ( clientSocket.getOutputStream ( ) );

		// Creación del flujo de entrada desde el servidor.
		inFromServer = new ObjectInputStream ( clientSocket.getInputStream ( ) );
	}

	/**
	 * Execute command in the operative system.
	 * @param command To execute
	 */
	private void execute(String command) {
		Runtime r = Runtime.getRuntime();
		Process p = null;

		try {
			p = r.exec(command);

			String line;

			BufferedReader input = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			while ((line = input.readLine()) != null) {
				System.out.println(line);
			}
			input.close();
		} catch (Exception e) {
			System.err.println("Error while executing " + command);
		}
	}

	/**
	 * Execution
	 */
	public static void main ( String args [ ] ) throws Exception
	{
		if (args.length > 1) {
			String address = args[0];
			int port = Integer.parseInt(args[1]);
			new TCPClient(address, port);
		} else {
			new TCPClient();
		}
	}
}
