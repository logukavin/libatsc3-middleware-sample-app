package org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model;

import java.util.List;

public class GetFilterCodes {
    public List<Filters> filters;
    public static class Filters{
        public Integer filterCode;
        public String expires;

        @Override
        public String toString() {
            return "{ \"filterCode\": "+filterCode+", \"expires\": \""+expires+"\" }";
        }
    }

    @Override
    public String toString() {
        return "{\"jsonrpc\": \"2.0\","+
                "\"result\": {"+
                "\"filters=\"" + getListToString() +
                '}';
    }

    private String getListToString() {
        String array ="[";
            if(filters==null && filters.size()==0){
                return array+"]";
            }else{
                for(int i=0;i<filters.size();i++){
                    array = array+filters.get(i);
                    if(i!=filters.size()-1){
                        array = array+",";
                    }
                }
                return array+"]";
            }
    }
}
