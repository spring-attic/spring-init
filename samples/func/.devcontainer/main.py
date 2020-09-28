# Copyright (c) 2013, Thomas P. Robitaille
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice,
# this list of conditions and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright notice,
# this list of conditions and the following disclaimer in the documentation
# and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

from __future__ import (unicode_literals, division, print_function,
                        absolute_import)

import time
import argparse


def get_percent(process):
    try:
        return process.cpu_percent()
    except AttributeError:
        return process.get_cpu_percent()


def get_memory(process):
    try:
        return process.memory_info()
    except AttributeError:
        return process.get_memory_info()


def all_children(pr):
    processes = []
    children = []
    try:
        children = pr.children()
    except AttributeError:
        children = pr.get_children()
    except Exception:  # pragma: no cover
        pass

    for child in children:
        processes.append(child)
        processes += all_children(child)
    return processes


def main():

    parser = argparse.ArgumentParser(
        description='Record CPU and memory usage for a process')

    parser.add_argument('process_id_or_command', type=str,
                        help='the process id or command')

    parser.add_argument('--log', type=str,
                        help='output the statistics to a file')

    parser.add_argument('--plot', type=str,
                        help='output the statistics to a plot')

    parser.add_argument('--plottitle', type=str,
                        help='title of the plot')

    parser.add_argument('--duration', type=float,
                        help='how long to record for (in seconds). If not '
                             'specified, the recording is continuous until '
                             'the job exits.')

    parser.add_argument('--interval', type=float,
                        help='how long to wait between each sample (in '
                             'seconds). By default the process is sampled '
                             'as often as possible.')

    parser.add_argument('--include-children',
                        help='include sub-processes in statistics (results '
                             'in a slower maximum sampling rate).',
                        action='store_true')

    args = parser.parse_args()

    # Attach to process
    try:
        pid = int(args.process_id_or_command)
        print("Attaching to process {0}".format(pid))
        sprocess = None
    except Exception:
        import subprocess
        command = args.process_id_or_command
        print("Starting up command '{0}' and attaching to process"
              .format(command))
        sprocess = subprocess.Popen(command, shell=True)
        pid = sprocess.pid

    monitor(pid, logfile=args.log, plot=args.plot, duration=args.duration,
            interval=args.interval, include_children=args.include_children, plottitle=args.plottitle)

    if sprocess is not None:
        sprocess.kill()


def monitor(pid, logfile=None, plot=None, duration=None, interval=None,
            include_children=False, plottitle=None):

    # We import psutil here so that the module can be imported even if psutil
    # is not present (for example if accessing the version)
    import psutil

    pr = psutil.Process(pid)

    # Record start time
    start_time = time.time()

    if logfile:
        f = open(logfile, 'w')
        f.write("# {0:12s} {1:12s} {2:12s} {3:12s}\n".format(
            'Elapsed time'.center(12),
            'CPU (%)'.center(12),
            'Real (MB)'.center(12),
            'Virtual (MB)'.center(12))
        )

    log = {}
    log['times'] = []
    log['cpu'] = []
    log['mem_real'] = []
    log['mem_virtual'] = []

    try:

        # Start main event loop
        while True:

            # Find current time
            current_time = time.time()

            try:
                pr_status = pr.status()
            except TypeError:  # psutil < 2.0
                pr_status = pr.status
            except psutil.NoSuchProcess:  # pragma: no cover
                break

            # Check if process status indicates we should exit
            if pr_status in [psutil.STATUS_ZOMBIE, psutil.STATUS_DEAD]:
                print("Process finished ({0:.2f} seconds)"
                      .format(current_time - start_time))
                break

            # Check if we have reached the maximum time
            if duration is not None and current_time - start_time > duration:
                break

            # Get current CPU and memory
            try:
                # PATCH of psrecord - divide by cpu-count
                current_cpu = get_percent(pr) / psutil.cpu_count()
                current_mem = get_memory(pr)
            except Exception:
                break
            current_mem_real = current_mem.rss / 1024. ** 2
            current_mem_virtual = current_mem.vms / 1024. ** 2

            # Get information for children
            if include_children:
                for child in all_children(pr):
                    try:
                        current_cpu += get_percent(child)
                        current_mem = get_memory(child)
                    except Exception:
                        continue
                    current_mem_real += current_mem.rss / 1024. ** 2
                    current_mem_virtual += current_mem.vms / 1024. ** 2

            if logfile:
                f.write("{0:12.3f} {1:12.3f} {2:12.3f} {3:12.3f}\n".format(
                    current_time - start_time,
                    current_cpu,
                    current_mem_real,
                    current_mem_virtual))
                f.flush()

            if interval is not None:
                time.sleep(interval)

            # If plotting, record the values
            if plot:
                log['times'].append(current_time - start_time)
                log['cpu'].append(current_cpu)
                log['mem_real'].append(current_mem_real)
                log['mem_virtual'].append(current_mem_virtual)

    except KeyboardInterrupt:  # pragma: no cover
        pass

    if logfile:
        f.close()

    if plot:

        from matplotlib.ticker import (MultipleLocator, NullFormatter, ScalarFormatter)
        import matplotlib.pyplot as plt
        import numpy as np

        #plt.style.use('dark_background')

        fig, ax = plt.subplots(facecolor=(.18, .31, .31))
        fig.suptitle('CPU & Memory Utilisation', fontsize=14, fontweight='bold')

        ax.set_facecolor('aliceblue')

        if plottitle:
            ax.set_title(plottitle)

        # log plot
        ax.semilogy(log['times'], log['cpu'], '-', color='darkgreen', lw=1)
        ax.set_ylabel(str(psutil.cpu_count()) + ' CPUs (%)', color='darkgreen', fontweight='bold')
        ax.yaxis.set_major_formatter(ScalarFormatter())
        ax.yaxis.set_minor_formatter(NullFormatter())
        ax.set_yticks([1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 100])
        ax.set_ylim(1, 100)

        ax.set_xlabel('time (s)')
        ax2 = ax.twinx()

        # log plot
        ax2.semilogy(log['times'], log['mem_real'], '-', color='crimson', lw=2)
        ax2.set_ylabel('RSS Memory (MB)', color='crimson', fontweight='bold')
        ax2.yaxis.set_major_formatter(ScalarFormatter())
        ax2.yaxis.set_minor_formatter(NullFormatter())
        ax2.set_yticks([10, 20, 30, 40, 50, 100, 200, 300, 400, 500, 1000])
        ax2.set_ylim(10, 1000)

        # read time for the first request from file
        with open('target/plots/time-server-ready.txt', 'r') as reader:
            line = reader.readline()
            data = line.split(":")
            start_time = int(data[1])/1000
            if int(data[0]) == pid:
                # print start area
                ax.axvspan(0, start_time, facecolor='tomato', alpha=0.3, label='time to first request')
                
                # find rss at start time
                for i in range(len(log['times'])):
                    if log['times'][i] > start_time :
                        rss_at_start_time = int(log['mem_real'][i])
                        break
                
                # print start time annotation
                ax2.annotate(str(start_time) + ' s\n' + str(rss_at_start_time) + ' MB', 
                    xy=(start_time, rss_at_start_time), 
                    xytext=(0.2, 0.5), 
                    textcoords='axes fraction',
                    bbox = dict(boxstyle="round", fc="0.8"),
                    arrowprops=dict(arrowstyle="->", connectionstyle="arc3"))

        # read time for the first request from file
        with open('target/plots/time-load-test.txt', 'r') as reader:
            line = reader.readline()
            data = line.split(":")
            test_duration = int(data[1])/1000
            test_start = start_time + 1
            test_end = test_start + test_duration
            if int(data[0]) == pid:
                # print test area
                ax.axvspan(test_start, test_end, facecolor='teal', alpha=0.3, label='load test (requests: 5000, concurrency: 5)')

                # find rss at test end
                for i in range(len(log['times'])):
                    if log['times'][i] > test_end :
                        rss_at_test_end = int(log['mem_real'][i])
                        break
                
                # print test end annotation
                ax2.annotate(str(test_duration) + ' s\n' + str(rss_at_test_end) + ' MB', 
                    xy=(test_end, rss_at_test_end), 
                    xytext=(0.8, 0.5), 
                    textcoords='axes fraction',
                    bbox = dict(boxstyle="round", fc="0.8"),
                    arrowprops=dict(arrowstyle="->", connectionstyle="arc3"))



        ax.legend(loc='lower right', facecolor='#ffffff')
        ax2.grid()

        fig.savefig(plot)
