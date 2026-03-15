/**
 * MAP对象，模拟java.util.MAP功能
 *
 * 接口：
 * size()     获取MAP元素个数
 * isEmpty()    判断MAP是否为空
 * clear()     删除MAP所有元素
 * put(key, value)   向MAP中增加元素（key, value)
 * remove(key)    删除指定KEY的元素，成功返回True，失败返回False
 * get(key)    获取指定KEY的元素值VALUE，失败返回NULL
 * containsKey(key)  判断MAP中是否含有指定KEY的元素
 * values()    获取MAP中所有VALUE的数组（ARRAY）
 * keys()     获取MAP中所有KEY的数组（ARRAY）
 * putAll()   追加另一个map
 * entries()  返回MapEntry[]
 *
 * 例子：
 * var map = new Map();
 *
 * map.put("key", "value");
 * var val = map.get("key")
 *
 */
MapEntry=function(k,v){
    this.key=k;
    this.value=v;
    this.keyEquals=function(key2){
        if(this.key==key2){
            return true;
        }else{
            return false;
        }
    }
}
Map=function(){
 	this.elements = [];
}
Map.prototype={
    size:function(){
        return this.elements.length;
    },
    clear:function(){
    	delete this.elements;
        this.elements=new Array();
    },
		isEmpty:function(){
        return (this.elements==null||this.elements.length<=0);
    },
    put:function(k,v){
        var newEntry=new MapEntry(k,v);
        for(var i=0;i<this.elements.length;i++){
            var entry=this.elements[i];
            if(entry.keyEquals(k)){
                return;
            }
        }
        this.elements.push(newEntry);
    },
    get:function(k){
        for(var i=0;i<this.elements.length;i++){
            var entry=this.elements[i];
            if(entry.keyEquals(k)){
                return entry.value;
            }
        }
        return null;
    },
	remove:function(k){
        var entryPoped;
        for(var i=0;i<this.elements.length;i++){
            entryPoped=this.elements.pop();
            if(entryPoped.keyEquals(k)){
                break;
            }else{
                this.elements.unshift(entryPoped);
            }
        }
    },
    keys:function(){
        var keys=[];
        for(var i=0;i<this.elements.length;i++){
            keys.push(this.elements[i].key);
        }
        return keys;
    },
    values:function(){
        var values=[];
        for(var i=0;i<this.elements.length;i++){
            values.push(this.elements[i].value);
        }
        return values;
    },
    containsKey:function(k){
        for(var i=0;i<this.elements.length;i++){
            if(this.elements[i].keyEquals(k))
                return true;
        }
        return false;
    },
    putAll:function(map){
        if(map==null||typeof map!="object"|| !(map instanceof Map)){
            throw new Error("the object to be put should be a valid map");
        }
        for(var i=0;i<map.size();i++){
            this.put(map.elements[i].key,map.elements[i].value);
        }
    },
    entries:function(){
    	return this.elements;
    }
};


/**
 * HashMap对象，模拟java.util.HashMap功能
 * 该方法在数据量1000-50W时比Map高效100倍左右，大数据量时建议使用
 * 接口：
 * size()     获取HashMap元素个数
 * put(key, value)   向HashMap中增加元素（key, value)
 * remove(key)    删除指定KEY的元素，成功返回True，失败返回False
 * get(key)    获取指定KEY的元素值VALUE，失败返回NULL
 * containsKey(key)  判断HashMap中是否含有指定KEY的元素
 * containsValue(key)  判断HashMap中是否含有指定VALUE的元素
 * values()    获取HashMap中所有VALUE的数组（ARRAY）
 * keys()     获取HashMap中所有KEY的数组（ARRAY）
 * clear()     删除HashMap所有元素
 *
 * 例子：
 * var map = new HashMap();
 *
 * map.put("key", "value");
 * var val = map.get("key")
 * 
 * 使用自定义对象最key值时，需要为自定义对象提供toString方法
 *
 */
HashMap = function() {
    /** Map 大小 **/
    var size = 0;
    /** 对象 **/
    var entry = new Object();

    /** 存 **/
    this.put = function(key , value) {
        if(!this.containsKey(key)) {
            size++ ;
        }
        entry[key] = value;
    }

    /** 取 **/
    this.get = function(key) {
        return this.containsKey(key) ? entry[key] : null;
    }

    /** 删除 **/
    this.remove = function(key) {
        if(this.containsKey(key) && (delete entry[key])) {
            size--;
        }
    }

    /** 是否包含 Key **/
    this.containsKey = function(key) {
        return (key in entry);
    }

    /** 是否包含 Value **/
    this.containsValue = function(value) {
        for(var prop in entry) {
            if(entry[prop] == value) {
                return true;
            }
        }
        return false;
    }

    /** 所有 Value **/
    this.values = function() {
        var values = new Array();
        for(var prop in entry) {
            values.push(entry[prop]);
        }
        return values;
    }

    /** 所有 Key **/
    this.keys = function() {
        var keys = new Array();
        for(var prop in entry) {
            keys.push(prop);
        }
        return keys;
    }

    /** Map Size **/
    this.size = function(){
        return size;
    }

    /* 清空 */
    this.clear = function(){
        size = 0;
        entry = new Object();
    }
}