**写在前面**

cassandra3.x官方文档的非官方翻译。翻译内容水平全依赖本人英文水平和对cassandra的理解。所以强烈建议阅读英文版[cassandra 3.x 官方文档](http://docs.datastax.com/en/cassandra/3.0/)。此文档一半是翻译，一半是个人对cassandra的认知。尽量将我的理解通过引用的方式标注，以示区别。另外文档翻译是项长期并有挑战的工作，如果你愿意加入[cassandra git book](https://www.gitbook.com/book/fs1360472174/cassandra-document/details),可以发信给我。当然你也可以加入我们的QQ群,104822562。一起学习探讨cassandra.
探测器决定了数据中心和机架节点的归属。他们将网络拓扑结构告知给Cassandra,因此请求可以比较高效的进行路由允许Cassandra通过将机器按照数据中心和机架进行分组从而分发副本。具体来说，复制策略基于新的探测器提供的信息放置副本。所有的节点必须返回相同的机架和数据中心信息。Cassandra 尽其所能，不将多个副本放在同一个机架上。(这不一定指代物理位置)

**Note:** 如果你更改探测器，你可能需要执行额外的步骤，因为探测器会影响副本放置。详情查看[更换探测器](http://docs.datastax.com/en/cassandra/3.0/cassandra/operations/opsSwitchSnitch.html)

**一.动态探测**

默认情况下，所有的探测器通过一个动态探测层来监控读延迟，尽可能不将请求路由到性能差的节点上。动态探测默认情况下是被启动的，同时也是适用于大多数的部署。想看看他是如何工作的，可以查看[http://www.datastax.com/dev/blog/dynamic-snitching-in-cassandra-past-present-and-future](http://www.datastax.com/dev/blog/dynamic-snitching-in-cassandra-past-present-and-future "Dynamic snitching in Cassandra:past, present, and future").可以cassandra.yaml文件中为每个节点配置动态探测阈值。

更多详细的内容，可以查看[http://docs.datastax.com/en/cassandra/3.0/cassandra/architecture/archDataDistributeFailDetect.html](http://docs.datastax.com/en/cassandra/3.0/cassandra/architecture/archDataDistributeFailDetect.html "Failure detection and recovery")列出的属性

**二.简单探测**

SimpleSnitch(默认值)只适用于单数据中心的部署。它不能识别数据中心或者机架信息，且只可以用于单数据中心的部署或者公有云的单地区。它将策略的顺序作为距离，可以提高缓存当禁掉读修复。

使用简单探测器时，在定义keyspace的时候，使用SimpleStrategy，然后指定一个复制因子。

**三.RackInferringSnitch**

RackInferringSnitch 通过机架和数据中心来决定节点的距离，分别和节点ip地址的第三位、第二位对应。这个探测器时用来写自定义探测器最好的例子。
![](http://docs.datastax.com/en/cassandra/3.0/cassandra/images/arc_rack_inferring_snitch_ips.png)
(除非这正好匹配你的部署协议)

**注:**
> 这个探测器实现起来非常粗暴，就是取ip
> `public String getRack(InetAddress endpoint)
    {
        return Integer.toString(endpoint.getAddress()[2] & 0xFF, 10);
    }`


**四.PropertyFileSnitch**

这个探测器通过机架和数据中心来决定节点的距离。使用cassandra-topology.properties 文件中定义的网络拓扑细节。当使用这个探测器可以将数据中心的名字定义为任何你想要的。确保定义keyspace时指定的名字和这边定义的一样。集群中的每个节点都应该在cassandra-topology.properties文件中定义。而且集群中的每个节点这个文件都应该一样。

**过程**

如果你的节点有有不一样ip，集群中有两个物理数据中心每个数据中心都有两个机架。第三个逻辑数据中心用来复制分析数据。配置文件可能看起来像下面这样：


**Note:** 数据中心和机架的名字是大小写敏感的.

   
    # datacenter One
    
    175.56.12.105=DC1:RAC1
    175.50.13.200=DC1:RAC1
    175.54.35.197=DC1:RAC1
    
    120.53.24.101=DC1:RAC2
    120.55.16.200=DC1:RAC2
    120.57.102.103=DC1:RAC2
    
    # datacenter Two
    
    110.56.12.120=DC2:RAC1
    110.50.13.201=DC2:RAC1
    110.54.35.184=DC2:RAC1
    
    50.33.23.120=DC2:RAC2
    50.45.14.220=DC2:RAC2
    50.17.10.203=DC2:RAC2
    
    # Analytics Replication Group
    
    172.106.12.120=DC3:RAC1
    172.106.12.121=DC3:RAC1
    172.106.12.122=DC3:RAC1
    
    # default for unknown nodes 
    default =DC3:RAC1


**注：**
> 这种配置方式应该是比较常见的方式，笔者也常常这么干。清晰明了，简单易懂。唯一的问题在于进行节点扩展时，需要更新所有的节点上此配置文件。不过不用重启节点令配置生效。默认刷新的时间是5s
>     
>      org.apache.cassandra.locator.PropertyFileSnitch
>      private static final int DEFAULT_REFRESH_PERIOD_IN_SECONDS = 5;


**五.Ec2Snitch**

集群中的所有节点都在一个地区，这种简单的集群部署在Amazon EC2可以使用Ec2Snitch方法。

在EC2上的部署，地区(region)的名字作为数据中心的名字。区域(zones)被当做数据中心中的机架。例如，如果一个节点在us-east-1区域，us-east是数据中心的名字，1是机架的位置。(机架对于分发副本很重要，而不是为了数据中心的命名)因为使用的是私有IPs,所以探测器无法跨地区。

如果你只使用单数据中心，不需要指定任何的属性。

如果使用多数据中心，需要在cassandra-rackdc.properties配置文件中设置dc_suffix选项。其他行会被忽略。

例如，us-east地区的每个节点，在cassandra-rackdc.properties文件中指定数据中心。

**Note：** 数据中心名字是大小写敏感的

- node0
	
	dc_suffix=_1_cassandra

- node1

	dc_suffix=_1_cassandra

- node2

	dc_suffix=_1_cassandra

- node3

	dc_suffix=_1_cassandra

- node4

	dc_suffix=_1_analytics

- node5

	dc_suffix=_1_search

这样会为该地区生成三个数据中心

    us-east_1_cassandra
	us-east_1_analytics
	us-east_1_search

**Note:** 在这个例子中，数据中心命名习惯是根据负载性质来定的。你可以使用其他的规范，如DC1，DC2,100，200.

**Keyspace Strategy 选项**

当定义keyspace strategy 选项，使用EC 地区名字，如'us-east'作为数据中心的名字

**注:**
> 亚马逊是云主机，提供给用户的只有region 和zone的概念。分别对应着cassandra的数据中心和机架，确保数据不会被放在一起。可以在[AWS regions](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html)`查看regions信息。


**六.Ec2MultiRegionSnitch**

当在Amazon EC2中的cassandra集群需要跨多地区的时候，使用Ec2MultiRegionSnitch。

当使用Ec2MultiRegionSnitch时,必须要在cassandra.yaml文件和属性文件cassandra-rackdc.properties中配置设置。

**cassandra.yaml文件配置跨区域通信**

Ec2MultiRegionSnitch 指定**broadcast_address**值为public IP,以此来允许跨地区的连接。将每个节点配置如下:

1. 在cassandra.yaml文件，设置listen_address 为节点的私有IP地址，broadcast_address设置为节点的public IP.

这样可以使得在EC2 某个region的Cassandra 节点可以绑定到另外的region,从而支持了多数据中心。对于region内部的流量，Cassandra会切换到private IP建立连接。

2. 在cassandra.yaml文件中设置seed nodes为public IP.私有IP不会再网络间被路由到。如:

	`seeds: 50.34.16.33, 60.247.70.52`

对于EC2中每一个seed nodes,可以通过下面指令找到public IP 地址

	`curl http://instance-data/latest/meta-data/public-ipv4`

**Note:**不要讲所有的节点都作为seeds,具体查看[gossip](http://docs.datastax.com/en/cassandra/3.0/cassandra/architecture/archGossipAbout.html)

3. 确保 storage_port(7000)或者ssl_storage_port(7001)没有被防火墙屏蔽

**配置snitch跨地区通信**

在EC2部署，地区(region)的名字作为数据中心的名字。区域(zones)被当做数据中心中的机架。例如，如果一个节点在us-east-1区域，us-east是数据中心的名字，1是机架的位置。(机架对于分发副本很重要，而不是为了数据中心的命名)

对于每个节点，需要在cassandra-rackdc.properties文件中指定它的数据中心。dc_suffix 选项定力了snitch将用到的数据中心。其他行会被忽略。

在下面的例子中，这儿有两个cassandra数据中心，每个数据中心根据负载命名。在这个例子中，数据中心命名习惯是根据负载性质来定的。你可以使用其他的规范，如DC1，DC2,100，200.(数据中心的名字大小写敏感)


<table cellpadding="4" cellspacing="0" summary="" id="archSnitchEC2MultiRegion__example-table" class="table" frame="border" border="1" rules="all"><colgroup><col style="width:50%"><col style="width:50%"></colgroup><thead class="thead" style="text-align:left;">
            <tr>
              <th class="entry cellrowborder" style="vertical-align:top;" id="d4154e149">Region: us-east</th>

              <th class="entry cellrowborder" style="vertical-align:top;" id="d4154e152">Region: us-west</th>

            </tr>

          </thead>
<tbody class="tbody">
            <tr>
              <td class="entry cellrowborder" style="vertical-align:top;" headers="d4154e149 ">Node and datacenter:<ul class="ul">
                  <li class="li"><strong class="ph b">node0</strong>
                    <p class="p"><code class="ph codeph">dc_suffix=_1_cassandra</code></p>
</li>

                  <li class="li"><strong class="ph b">node1</strong><p class="p"><code class="ph codeph">dc_suffix=_1_cassandra</code></p>
</li>

                  <li class="li"><strong class="ph b">node2</strong><p class="p"><code class="ph codeph">dc_suffix=_2_cassandra</code></p>
</li>

                  <li class="li"><strong class="ph b">node3</strong>
                    <p class="p"><code class="ph codeph">dc_suffix=_2_cassandra</code></p>
</li>

                  <li class="li"><strong class="ph b">node4</strong>
                    <p class="p"><code class="ph codeph">dc_suffix=_1_analytics</code></p>
</li>

                  <li class="li"><strong class="ph b">node5</strong>
                    <p class="p"><code class="ph codeph">dc_suffix=_1_search</code></p>
</li>

                </ul>
<div class="p">This results in four <span class="keyword parmname">us-east</span> datacenters:<pre class="pre codeblock no-highlight"><code>us-east_1_cassandra
us-east_2_cassandra
us-east_1_analytics
us-east_1_search</code></pre></div>
</td>

              <td class="entry cellrowborder" style="vertical-align:top;" headers="d4154e152 ">Node and datacenter:<ul class="ul">
                  <li class="li"><strong class="ph b">node0</strong><p class="p"><code class="ph codeph">dc_suffix=_1_cassandra</code></p>
</li>

                  <li class="li"><strong class="ph b">node1</strong><p class="p"><code class="ph codeph">dc_suffix=_1_cassandra</code></p>
</li>

                  <li class="li"><strong class="ph b">node2</strong><p class="p"><code class="ph codeph">dc_suffix=_2_cassandra</code></p>
</li>

                  <li class="li"><strong class="ph b">node3</strong>
                    <p class="p"><code class="ph codeph">dc_suffix=_2_cassandra</code></p>
</li>

                  <li class="li"><strong class="ph b">node4</strong><p class="p"><code class="ph codeph">dc_suffix=_1_analytics</code></p>
</li>

                  <li class="li"><strong class="ph b">node5</strong>
                    <p class="p"><code class="ph codeph">dc_suffix=_1_search</code></p>
</li>

                </ul>
<div class="p">This results in four <span class="keyword parmname">us-west</span> datacenters:<pre class="pre codeblock no-highlight"><code>us-west_1_cassandra
us-west_2_cassandra
us-west_1_analytics
us-west_1_search</code></pre></div>
</td>

            </tr>

          </tbody>
</table>

**Keyspace Strategy 选项**

当定义keyspace strategy 选项，使用EC 地区名字，如'us-east'作为数据中心的名字。

    相关的信息
    [install locationl](http://docs.datastax.com/en/cassandra/3.0/cassandra/install/referenceInstallLocationsTOC.html)



**七.GoogleCloudSnitch**

可以使用GoogleCloudSnitch为[Google Cloud Platform](https://cloud.google.com/)提供单个地区或多地区的Cassandra 部署。地区作为数据中心，区域(zones)被当做数据中心中的机架。在相同逻辑网络中所有的通信都通过私有IP地址进行通信。

region名字作为数据中心名字，zones作为数据中心里面的机架。例如，如果一个节点在us-central1-a region，us-central1是数据中心的名字，a是机架的位置(机架对于分发副本很重要，而不是为了数据中心的命名)这种snitch可以跨region而不需要额外的配置。

如果你只使用单数据中心，不需要指定任何的属性。

如果使用多数据中心，需要在cassandra-rackdc.properties配置文件中设置dc_suffix选项。其他行会被忽略。

例如，us-central1地区的每个节点，在cassandra-rackdc.properties文件中指定数据中心。

**Note:** 数据中心名字大小写敏感

- node0
	
	dc_suffix=_a_cassandra

- node1

	dc_suffix=_a_cassandra

- node2

	dc_suffix=_a_cassandra

- node3

	dc_suffix=_a_cassandra

- node4

	dc_suffix=_a_analytics

- node5
	
	dc_suffix=_a_search

**Note:** 数据中心名字和机架名字大小写敏感

**八.CloudstackSnitch **

在[Apache Cloudstack](http://cloudstack.apache.org/)环境可以使用CloudstackSnitch.因为zone的命名是自由格式的，这种探测器使用广泛使用的<country> <location> <az>标记