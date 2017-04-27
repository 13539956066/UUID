
namespace java com.xiaojing.uuid.bean

struct Interval{
  1:required i32 dataCenterId
  2:required i32 workerId
  3:required i64 startTime
  4:required i64 endTime
  5:required i32 dataCenterBits
  6:required i32 workerBits
  7:required i32 sequenceBits
}



service UUID{

  /**
   * 租用接口,必须先申请租用,然后根据返回值的datacenter和worker的id来确定uuid的组成
  **/
  Interval rentInterval(1:required string nameSpace, 2:required i64 duration);

  /**
   * 续租接口,在租期快到的时候,如果需要继续使用,则需要调用此接口,进行续租
   *
  **/
  Interval renewal(1:required Interval interval, 2:required i64 duration);

  /**
   * 退租接口,必须先停止uuid的生成,然后再调用此接口
   * 对同一个client来说,一旦调用过退租接口,想再次使用时,必须重新申请id
   * todo 此接口不一定需要
  **/
  Interval cancel(1:required Interval interval);

}