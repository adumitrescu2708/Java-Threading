import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @for Tema2 - APD - Java Threading
 * @author Dumitrescu Alexandra
 * @since Dec 2022
 * */
public class Tema2 {
	/** Strings used for input parsing */
	public static String input_folder;
	public static String input_orders_file;
	public static String output_orders_file;
	public static String input_products_file;
	public static String output_products_file;

	/** BONUS - Number of bytes in the orders.txt input file
	 * used to split the number of bytes equally for each thread
	 * <>
	 *    Suppose we have P maximum threads on layer 1 and N bytes in the input file.
	 *    We first split the array in equal chunks of bytes for each thread (from 1 : P)
	 *    Each thread reads byte with byte from its chunk starting from the next available
	 *    line and reads until it reaches the end of its chunk and finishes the line
	 *
	 *        start_chunk
	 *    <>    |
	 *        b l a b l a b l a
	 *        b l a b l a b l a
	 *    </>       |
	 *            end_chunk
	 *
	 *     In the previous case, the thread jumps to the first newline (2nd row) and reads the
	 *     entire line (until the next newline)
	 * </>
	 * */
	public static long input_file_bytes;

	/** Number of threads */
	public static int maximum_threads;

	/** Semaphore used for enabling only
	 * <Number of threads> threads in the level 2 */
	public static Semaphore layer2Enable;

	/** Semaphore used for enabling writing in the
	 *  order_products_out.txt in order to avoid race condition */
	public static Semaphore writeEnableProduct;

	/** Semaphore used for enabling writing in the orders_out.txt
	 *  in order to avoid race condition */
	public static Semaphore writeEnableOrder;

	/** Semaphore with one permit that enables incrementing
	 *  the counter of 1st layer threads that added tasks in the pool */
	public static Semaphore incrementCounterEnable;

	/** Layer 2 implements the logic of replicated workers pattern.
	 *  Each thread that ships an order adds <number_of_products>
	 *  tasks in the pool */
	public static AtomicInteger inQueue;
	public static ExecutorService service;

	/** Semaphores used for each order */
	public static Semaphore[] semaphores;
	/** Counter for all 1st layer threads that have added tasks in the pool*/
	public static int counter;

	/** File writers used for writing the results */
	public static FileWriter product_writer;
	public static FileWriter order_writer;

	public static void main(String[] args) throws IOException {
		if(args.length < 2) {
			System.out.println("Bad usage! Try: java *.java <folder_input> <nr_max_threads>");
		} else {
			/** Parse the input from the given parameters */
			counter = 0;
			input_folder = args[0];
			maximum_threads = Integer.parseInt(args[1]);
			input_orders_file = input_folder + "/orders.txt";
			input_products_file = input_folder + "/order_products.txt";
			output_orders_file = "./orders_out.txt";
			output_products_file = "./order_products_out.txt";

			/** Prepare the Executors Service for the 2nd layer threads */
			service = Executors.newFixedThreadPool(maximum_threads);
			inQueue = new AtomicInteger(0);

			/** Get the number of total bytes for the 1st layer threads equal distribution */
			RandomAccessFile input_file = new RandomAccessFile(input_orders_file, "r");
			input_file_bytes = input_file.length();
			input_file.close();

			/** Create and open the output files */
			File output_file = new File(output_orders_file);
			output_file.createNewFile();

			File products_file = new File(output_products_file);
			products_file.createNewFile();

			/** Prepare the shared output writers */
			order_writer = new FileWriter(output_file);
			product_writer = new FileWriter(products_file);

			/** List of semaphores.
			 *  A 1st layer thread will finish each order one all
			 *  tasks added for that order all completed.
			 *  In this way, we use a semaphore to announce
			 *  when to exit the waiting mode, once all tasks are finished.
			 * */
			semaphores = new Semaphore[maximum_threads + 1];


			/** Let only one thread at a time write in the output files */
			writeEnableProduct = new Semaphore(1);
			writeEnableOrder = new Semaphore(1);
			incrementCounterEnable = new Semaphore(1);

			/** Let only maximum_threads in the 2nd layer */
			layer2Enable = new Semaphore(maximum_threads);

			/** Start the 1st layer threads */
			first_layer_threads();

			/** Close the writers after finishing */
			order_writer.close();
			product_writer.close();
		}
	}

	public static void first_layer_threads()
	{
		/** Initialize 1st layer threads */
		Layer_1Worker[] threads = new Layer_1Worker[maximum_threads];

		/** Start the threads */
		for(int i = 0; i < maximum_threads; i++) {
			threads[i] = new Layer_1Worker(i);
			threads[i].start();
		}

		/** Wait for the threads to finish */
		for(int i = 0; i < maximum_threads; i++) {
			try {
				threads[i].join();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}