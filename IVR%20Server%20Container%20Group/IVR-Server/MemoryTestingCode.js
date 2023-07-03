const os = require('os');
const fs = require('fs');

function logMemoryUsage() {
  const totalMemory = os.totalmem();
  const freeMemory = os.freemem();
  const usedMemory = totalMemory - freeMemory;

  const memoryUsage = {
    total: formatBytes(totalMemory),
    used: formatBytes(usedMemory),
    free: formatBytes(freeMemory),
  };

  const logMessage = `RAM Memory Usage: ${JSON.stringify(memoryUsage)}`;

  return logMessage;
}

function formatBytes(bytes) {
  const kilobytes = bytes / (1024 * 1024);
  return `${kilobytes.toFixed(2)} MB`;
}

function logHeapMemory() {
  const memoryUsage = process.memoryUsage();
  const hepMemoryUsage = {
    total: formatBytes(memoryUsage.heapTotal),
    used: formatBytes(memoryUsage.heapUsed),
    rss: formatBytes(memoryUsage.rss)
  };
  const logMessage = `Heap Memory Usage: ${JSON.stringify(hepMemoryUsage)}`;
  return logMessage
}

function log(){
  const logMessage = logMemoryUsage()
  const heapMemory = logHeapMemory()
  const finalResult = `${logMessage} \n ${heapMemory} \n\n`
  fs.appendFile('memory.txt', finalResult, (err) => {
    if (err) {
      console.error('Error writing to log file:', err);
    }
  });
}

// Log memory usage every minute
setInterval(log, 10000);
