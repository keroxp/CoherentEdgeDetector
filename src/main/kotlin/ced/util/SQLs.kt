package ced.util

object SQLs {
    public fun hashToInsertQuery(hash: Map<String, Any>, table: String): String {
        val schema = StringBuilder()
        val values = StringBuilder()
        for (it in hash) {
            schema.append("'${it.key}',")
            values.append("'${it.value}',")
        }
        schema.deleteCharAt(schema.length-1)
        values.deleteCharAt(values.length-1)
        return "INSERT INTO $table(${schema.toString()}) VALUES(${values.toString()});"
    }
}